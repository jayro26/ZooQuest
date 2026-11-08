package main;

import javax.swing.*;
import java.awt.*;

/**
 * Main menu. Mirrors MenuCoordinator.mainMenu(): Start Game, Achievements, View
 * Profile, Exit.
 */
public class MainMenuPanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private final ZooQuestGUI app;
    private final JLabel welcome = new JLabel("", SwingConstants.CENTER);
    private final JLabel progress = new JLabel("", SwingConstants.CENTER);

    public MainMenuPanel(ZooQuestGUI app) {
        super("jungle_scene.png");
        this.app = app;
        JPanel card = ZooQuestGUI.card(34);

        JLabel title = new JLabel("MAIN MENU");
        title.setFont(new Font("SansSerif", Font.BOLD, 40));
        title.setForeground(ZooQuestGUI.GOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);

        welcome.setFont(new Font("SansSerif", Font.BOLD, 20));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(CENTER_ALIGNMENT);
        progress.setFont(new Font("SansSerif", Font.PLAIN, 14));
        progress.setForeground(new Color(210, 230, 210));
        progress.setAlignmentX(CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(welcome);
        card.add(Box.createVerticalStrut(4));
        card.add(progress);
        card.add(Box.createVerticalStrut(24));
        addButton(card, "Start Game", new Color(46, 150, 80), () -> app.show(ZooQuestGUI.BIOMES));
        addButton(card, "Achievements", new Color(200, 140, 30), () -> app.show(ZooQuestGUI.ACHIEVEMENTS));
        addButton(card, "View Profile", new Color(50, 120, 190), () -> app.show(ZooQuestGUI.PROFILE));
        addButton(card, "Exit", new Color(170, 60, 60), app::exitGame);
        add(card);
    }

    private void addButton(JPanel card, String text, Color color, Runnable action) {
        JButton b = ZooQuestGUI.button(text, color);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.addActionListener(e -> action.run());
        card.add(b);
        card.add(Box.createVerticalStrut(12));
    }

    /** Called every time the menu appears, so progress is always current. */
    @Override
    public void onShow() {
        UserProfile p = app.getGameManager().getCurrentPlayer();
        if (p == null)
            return;
        welcome.setText("Welcome, " + p.getUsername() + "!");
        progress.setText(p.showProgress());
    }
}