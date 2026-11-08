package main;

import javax.swing.*;
import java.awt.*;

/** First screen: title + name entry. Mirrors MenuCoordinator.startSession(). */
public class StartPanel extends BackgroundPanel {
    private static final String DEFAULT_NAME = "Player";
    private final JTextField nameField = new JTextField(16);
    private final JLabel error = new JLabel(" ", SwingConstants.CENTER);

    public StartPanel(ZooQuestGUI app) {
        super("jungle_scene.png");
        JPanel card = ZooQuestGUI.card(34);

        JLabel title = new JLabel("ZOO QUEST");
        title.setFont(new Font("SansSerif", Font.BOLD, 52));
        title.setForeground(ZooQuestGUI.GOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Find the hidden animals!");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 18));
        sub.setForeground(Color.WHITE);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JLabel prompt = new JLabel("Enter your name (blank = " + DEFAULT_NAME + ")");
        prompt.setForeground(Color.WHITE);
        prompt.setFont(new Font("SansSerif", Font.PLAIN, 15));
        prompt.setAlignmentX(CENTER_ALIGNMENT);

        nameField.setFont(new Font("SansSerif", Font.PLAIN, 20));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setMaximumSize(new Dimension(260, 42));
        nameField.setAlignmentX(CENTER_ALIGNMENT);

        error.setForeground(new Color(255, 140, 120));
        error.setAlignmentX(CENTER_ALIGNMENT);

        JButton play = ZooQuestGUI.button("Let's Go!", new Color(46, 150, 80));
        play.setAlignmentX(CENTER_ALIGNMENT);
        Runnable go = () -> {
            String name = nameField.getText();
            try {
                app.getGameManager().initializeGame(name.isBlank() ? DEFAULT_NAME : name); // restores save
                error.setText(" ");
                app.show(ZooQuestGUI.MENU);
            } catch (IllegalArgumentException ex) { // e.g. name longer than 20 characters
                error.setText(ex.getMessage());
            }
        };
        play.addActionListener(e -> go.run());
        nameField.addActionListener(e -> go.run()); // Enter key

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(prompt);
        card.add(Box.createVerticalStrut(8));
        card.add(nameField);
        card.add(Box.createVerticalStrut(6));
        card.add(error);
        card.add(Box.createVerticalStrut(14));
        card.add(play);
        add(card);
    }
}
