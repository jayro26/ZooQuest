package main;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A panel that paints a picture "cover style" behind its children. Reusable by
 * every screen.
 */
public class BackgroundPanel extends JPanel {
    private final BufferedImage source;
    private BufferedImage cached;
    private int cachedW = -1, cachedH = -1;

    public BackgroundPanel(String imageFile) {
        this.source = ZooQuestGUI.loadImage(imageFile);
        setLayout(new GridBagLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        if (source == null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(34, 110, 60), 0, h, new Color(12, 50, 30)));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            return;
        }
        if (cached == null || cachedW != w || cachedH != h) { // scale once per window size
            double s = Math.max(w / (double) source.getWidth(), h / (double) source.getHeight());
            int sw = (int) Math.ceil(source.getWidth() * s), sh = (int) Math.ceil(source.getHeight() * s);
            cached = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = cached.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(source, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
            g2.dispose();
            cachedW = w;
            cachedH = h;
        }
        g.drawImage(cached, 0, 0, null);
    }
}
