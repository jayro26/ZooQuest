package main.UI;

import java.awt.*;
import java.awt.event.MouseAdapter;
import javax.swing.*;

/*
- The dimming layer: darkens the whole window and, for the loading screen, writes "Loading...." at the bottom left.
- It is used as the window's glass pane, so it also swallows every click while it is visible.
*/
final class LoadingOverlay extends JComponent {
    static final String TEXT = "Loading....";

    private final boolean showText;                     // "Loading...." at the bottom left
    private final float dim;                            // how black the dimming gets (0.5 = 50 %, 1 = fully black)
    private float amount = 0f;                          // 0 = clear, 1 = fully dimmed
    private Timer timer;

    LoadingOverlay(boolean showText, float dim) {
        this.showText = showText;
        this.dim = dim;
        setOpaque(false);
        addMouseListener(new MouseAdapter() {});         // swallow clicks, presses and hover underneath
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    // Smoothly moves the dimming to {@code target} (0 or 1) in {@code ms} milliseconds, then runs {@code done}.
    void fadeTo(float target, int ms, Runnable done) {
        if (timer != null) timer.stop();
        final float from = amount;
        final long[] last = { System.nanoTime() };
        final double[] elapsed = { 0 };
        timer = new Timer(15, null);
        timer.setCoalesce(true);
        timer.addActionListener(e -> {
            long now = System.nanoTime();
            elapsed[0] += Math.min(40.0, (now - last[0]) / 1e6);   // a slow frame must not skip the fade
            last[0] = now;
            double t = Math.min(1.0, elapsed[0] / Math.max(1, ms));
            amount = (float) (from + (target - from) * (t * t * (3 - 2 * t)));
            repaint();
            if (t >= 1.0) {
                ((Timer) e.getSource()).stop();
                if (done != null) done.run();
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (amount <= 0f) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(new Color(0f, 0f, 0f, dim * amount));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (!showText) { g.dispose(); return; }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, Math.max(18, Math.min(30, getHeight() / 26))));
        int x = 28, y = getHeight() - 28;                       // bottom left
        g.setColor(new Color(0f, 0f, 0f, 0.6f * amount));
        g.drawString(TEXT, x + 2, y + 2);
        g.setColor(new Color(1f, 1f, 1f, amount));
        g.drawString(TEXT, x, y);
        g.dispose();
    }
}
