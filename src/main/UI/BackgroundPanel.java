package main.UI;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; 
import javax.swing.*; 

// A panel that paints a picture "cover style" behind its children. Reusable by every screen.
public class BackgroundPanel extends JPanel implements Fadable {
    private static final Map<String, List<BufferedImage>> MIPMAPS = new HashMap<>();

    private final List<BufferedImage> mips; 
    private BufferedImage cached;
    private int cachedW = -1, cachedH = -1;
    private boolean sharp; // true when the cached copy was made with the best-quality (slower) scaling
    
    // fires 150 ms after the last size change and swaps in the best-quality copy
    private final Timer refineTimer = new Timer(150, e -> refine());

    public BackgroundPanel(String imageFile) {
        this.mips = mipmapsFor(imageFile); 
        setLayout(new GridBagLayout());
        refineTimer.setRepeats(false); 
    }

    // The elements stay inside the centred 16:9 stage while the picture behind them fills the whole panel.
    @Override
    public Insets getInsets() {
        Insets b = super.getInsets();
        Insets s = ZooQuestGUI.stageInsets(this);
        return new Insets(b.top + s.top, b.left + s.left, b.bottom + s.bottom, b.right + s.right);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        if (mips.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(34, 110, 60), 0, h, new Color(12, 50, 30)));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform tx = g2.getTransform();
        double sx = Math.max(1.0, tx.getScaleX()), sy = Math.max(1.0, tx.getScaleY());
        int pw = Math.max(1, (int) Math.ceil(w * sx)), ph = Math.max(1, (int) Math.ceil(h * sy));

        if (cached == null || cachedW != pw || cachedH != ph) { // scale once per window size
            cached = render(pw, ph, false); // fast while the size is changing
            cachedW = pw;
            cachedH = ph;
            sharp = false;
            refineTimer.restart(); // sharpen once the size has settled
        }
        g2.scale(1 / sx, 1 / sy); // draw the cache 1:1 on the screen pixels
        g2.drawImage(cached, 0, 0, null);
        g2.dispose();
    }

    // ---- fade support: only the children (buttons, logo, text...) fade; the picture behind them never does
    private boolean fadeEnabled;
    private float contentAlpha = 1f;
    private Timer fadeTimer;

    // Turns the fade in / fade out of this screen's elements on or off (off by default).
    public void setFadeEnabled(boolean on) {
        fadeEnabled = on;
        if (!on) setContentAlpha(1f);
    }

    public boolean isFadeEnabled() {
        return fadeEnabled;
    }

    public float getContentAlpha() {
        return contentAlpha;
    }

    // 0 = elements invisible, 1 = fully visible. Has no effect unless fading is enabled.
    public void setContentAlpha(float a) {
        contentAlpha = Math.max(0f, Math.min(1f, a));
        repaint();
    }

    // Smoothly changes the elements' opacity to {@code target} over {@code ms} milliseconds, then runs {@code done}.
    public void fadeTo(float target, int ms, Runnable done) {
        if (fadeTimer != null) fadeTimer.stop();
        final float from = contentAlpha;
        if (ms <= 0 || from == target) {
            setContentAlpha(target);
            if (done != null) done.run();
            return;
        }
        final long[] last = { System.nanoTime() };
        final double[] elapsed = { 0 };
        fadeTimer = new Timer(15, null);
        fadeTimer.setCoalesce(true);
        fadeTimer.addActionListener(e -> {
            long now = System.nanoTime();                              // a slow frame (e.g. a screen loading its pictures)
            elapsed[0] += Math.min(40.0, (now - last[0]) / 1e6);       // must not make the fade skip ahead
            last[0] = now;
            double t = Math.min(1.0, elapsed[0] / ms);
            double eased = t * t * (3 - 2 * t);                       // smooth start and end
            setContentAlpha((float) (from + (target - from) * eased));
            if (t >= 1.0) {
                ((Timer) e.getSource()).stop();
                if (done != null) done.run();
            }
        });
        fadeTimer.start();
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (!fadeEnabled || contentAlpha >= 0.999f) {
            super.paintChildren(g);
            return;
        }
        if (contentAlpha <= 0.001f) return;                           // fully faded out: draw only the picture
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(contentAlpha));
        super.paintChildren(g2);
        g2.dispose();
    }

    // replaces the cached copy with a best-quality one (runs once the window stops resizing).
    private void refine() {
        if (cached != null && !sharp) {
            cached = render(cachedW, cachedH, true);
            sharp = true;
            repaint();
        }
    }

    private BufferedImage render(int pw, int ph, boolean best) {
        return renderCover(mips, pw, ph, best);
    }

    // shared with ScenePanel, so every screen's background is scaled the same (good) way
    static BufferedImage renderCover(List<BufferedImage> mips, int pw, int ph, boolean best) {
        BufferedImage base = mips.get(0);
        double s = Math.max(pw / (double) base.getWidth(), ph / (double) base.getHeight());
        int sw = (int) Math.ceil(base.getWidth() * s), sh = (int) Math.ceil(base.getHeight() * s);

        BufferedImage from = base; // start from the smallest copy that is still at least as big as the target
        for (BufferedImage m : mips)
            if (m.getWidth() >= sw && m.getHeight() >= sh)
                from = m;

        BufferedImage out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, best ? RenderingHints.VALUE_INTERPOLATION_BICUBIC
                : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, best ? RenderingHints.VALUE_RENDER_QUALITY
                : RenderingHints.VALUE_RENDER_SPEED);
        g2.drawImage(from, (pw - sw) / 2, (ph - sh) / 2, sw, sh, null);
        g2.dispose();
        return out;
    }

    // loads the picture once (shared by every screen that uses it) and builds its half-size copies.
    static List<BufferedImage> mipmapsFor(String imageFile) {
        return MIPMAPS.computeIfAbsent(imageFile, file -> {
            BufferedImage src = ZooQuestGUI.loadImage(file);
            if (src == null)
                return List.of(); // picture not found: the gradient is painted instead

            List<BufferedImage> list = new ArrayList<>();
            BufferedImage cur = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = cur.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
            list.add(cur);
            while (cur.getWidth() / 2 >= 64 && cur.getHeight() / 2 >= 64) {
                BufferedImage half = new BufferedImage(cur.getWidth() / 2, cur.getHeight() / 2, BufferedImage.TYPE_INT_RGB);
                Graphics2D hg = half.createGraphics();
                hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                hg.drawImage(cur, 0, 0, half.getWidth(), half.getHeight(), null);
                hg.dispose();
                list.add(half);
                cur = half;
            }
            return list;
        });
    }
}