package main;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; 
import javax.swing.*; 


/**
 * A panel that paints a picture "cover style" behind its children. Reusable by
 * every screen.
 */
public class BackgroundPanel extends JPanel {
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

        // work in real screen pixels. sx/sy are 1.0 normally and 1.25 / 1.5 / 2.0 with display scaling.
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

    private void refine() {
        if (cached != null && !sharp) {
            cached = render(cachedW, cachedH, true);
            sharp = true;
            repaint();
        }
    }

    /* the picture scaled "cover style" to pw x ph real pixels (best quality when {@code best}). */
    private BufferedImage render(int pw, int ph, boolean best) {
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

    /* loads the picture once (shared by every screen that uses it) and builds its half-size copies. */
    private static List<BufferedImage> mipmapsFor(String imageFile) {
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