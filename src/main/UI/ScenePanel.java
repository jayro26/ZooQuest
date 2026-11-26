package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import main.Biome;

public class ScenePanel extends JPanel implements Fadable {
    private Biome biome;
    private BufferedImage cached;
    private int cachedW = -1, cachedH = -1;
    private String cachedFor = "";

    // ---- fade support: only the children (buttons, logo, text...) fade; the picture behind them never does
    private boolean fadeEnabled;
    private float contentAlpha = 1f;
    private Timer fadeTimer;

    // Turns the fade in / fade out of this screen's elements on or off (off by default).
    public void setFadeEnabled(boolean on) {
        fadeEnabled = on;
        if (!on) setContentAlpha(1f);
    }

    @Override
    public boolean isFadeEnabled() {
        return fadeEnabled;
    }

    @Override
    public float getContentAlpha() {
        return contentAlpha;
    }

    // 0 = elements invisible, 1 = fully visible. Has no effect unless fading is enabled.
    @Override
    public void setContentAlpha(float a) {
        contentAlpha = Math.max(0f, Math.min(1f, a));
        repaint();
    }

    // Smoothly changes the elements' opacity to {@code target} over {@code ms} milliseconds, then runs {@code done}. 
    @Override
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

    public ScenePanel() {
        setLayout(new BorderLayout());
    }

    public void setBiome(Biome b) {
        this.biome = b;
        cached = null;
        repaint();
    }

    public static Color top(String type) {
        switch (type) {
            case "Safari": return new Color(214, 140, 60);
            case "Aquatic": return new Color(40, 140, 210);
            default: return new Color(34, 110, 60);
        }
    }

    public static Color bottom(String type) {
        switch (type) {
            case "Safari": return new Color(120, 60, 20);
            case "Aquatic": return new Color(8, 40, 90);
            default: return new Color(12, 50, 30);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        String type = biome == null ? "Jungle" : biome.getBiomeType();
        BufferedImage src = biome == null ? null : GuiImages.load(biome.getBackgroundTemplate());
        Graphics2D g2 = (Graphics2D) g.create();
        if (src == null) {
            g2.setPaint(new GradientPaint(0, 0, top(type), 0, h, bottom(type)));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(-120, -140, 420, 420);
            g2.fillOval(w - 300, h - 280, 520, 520);
        } else {
            if (cached == null || cachedW != w || cachedH != h || !cachedFor.equals(biome.getBiomeName())) {
                double s = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
                int sw = (int) Math.ceil(src.getWidth() * s), sh = (int) Math.ceil(src.getHeight() * s);
                cached = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
                Graphics2D c = cached.createGraphics();
                c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                c.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
                c.dispose();
                cachedW = w; cachedH = h; cachedFor = biome.getBiomeName();
            }
            g2.drawImage(cached, 0, 0, null);
            g2.setColor(new Color(0, 0, 0, 60));   // slight darkening so text stays readable
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }
}
