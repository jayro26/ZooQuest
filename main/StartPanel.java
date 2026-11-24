package main;

import javax.swing.*;
import java.awt.*;

/** START screen: "Enter Your Name" + text field + START (layout PDF, page 1). */
public class StartPanel extends BackgroundPanel {
    private static final String DEFAULT_NAME = "Player";
    private final JTextField nameField = new JTextField(16);
    private final JLabel error = new JLabel(" ", SwingConstants.CENTER);

    public StartPanel(ZooQuestGUI app) {
        super("jungle_scene.png");
        JPanel card = ZooQuestGUI.card(26);

        JLabel prompt = new JLabel("Enter Your Name");
        prompt.setForeground(Color.WHITE);
        prompt.setFont(new Font("SansSerif", Font.PLAIN, 22));
        prompt.setAlignmentX(CENTER_ALIGNMENT);

        nameField.setFont(new Font("SansSerif", Font.PLAIN, 22));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setMaximumSize(new Dimension(380, 46));
        nameField.setPreferredSize(new Dimension(380, 46));
        nameField.setAlignmentX(CENTER_ALIGNMENT);

        error.setForeground(new Color(255, 140, 120));
        error.setFont(new Font("SansSerif", Font.PLAIN, 14));
        error.setAlignmentX(CENTER_ALIGNMENT);

        GuiKit.Btn start = new GuiKit.Btn("START", GuiKit.GREEN, 300, 48, 20);
        start.setAlignmentX(CENTER_ALIGNMENT);
        Runnable go = () -> {
            String name = nameField.getText();
            try {
                app.getGameManager().initializeGame(name.isBlank() ? DEFAULT_NAME : name); // restores save
                error.setText(" ");
                app.show(ZooQuestGUI.MENU);
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
            }
        };
        start.addActionListener(e -> go.run());
        nameField.addActionListener(e -> go.run());

        card.add(prompt);
        card.add(Box.createVerticalStrut(14));
        card.add(nameField);
        card.add(Box.createVerticalStrut(4));
        card.add(error);
        card.add(Box.createVerticalStrut(10));
        card.add(start);
        add(card);
    }
}
