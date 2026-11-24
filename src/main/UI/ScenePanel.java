package main.UI;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import main.*;

public class ScenePanel extends JPanel {
    private Biome biome;
    private BufferedImage cached;
    private int cachedW = -1, cachedH = -1;
    private String cachedFor = "";

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
