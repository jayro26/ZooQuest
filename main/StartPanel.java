package main;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import javax.swing.*;

/**
 * First screen: title + name entry. Mirrors MenuCoordinator.startSession().
 *
 * Left box : the name entry (always shown).
 * Right box: the saved players (shown only once some exist). Click a player to fill in the name, or
 *            double-click to start as that player.
 * Every player has their own save file, so progress and achievements are never shared between players.
 */
public class StartPanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private static final String DEFAULT_NAME = "Player";
    private static final int LIST_WIDTH = 260;
    private static final int LIST_HEIGHT = 150;      // about three players visible, the rest scrolls
    private static final int BOX_GAP = 24;           // space between the two boxes

    private final ZooQuestGUI app;
    private final JTextField nameField = new JTextField(16);
    private final JLabel error = new JLabel(" ", SwingConstants.CENTER);

    // right box: wrapped in its own panel, so it (and the gap) takes no space while there are no saved players
    private final DefaultListModel<GameManager.SavedPlayer> model = new DefaultListModel<>();
    private final JList<GameManager.SavedPlayer> playerList = new JList<>(model);
    private final JPanel savedColumn = new JPanel(new BorderLayout());

    public StartPanel(ZooQuestGUI app) {
        super(" ");
        this.app = app;

        // ---- left box: the name entry (same as the original start screen)
        JPanel card = ZooQuestGUI.card(34);

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

        JButton play = ZooQuestGUI.button("Start", new Color(46, 150, 80));
        play.setAlignmentX(CENTER_ALIGNMENT);
        play.addActionListener(e -> go());
        nameField.addActionListener(e -> go()); // Enter key

        card.add(prompt);
        card.add(Box.createVerticalStrut(8));
        card.add(nameField);
        card.add(Box.createVerticalStrut(6));
        card.add(error);
        card.add(Box.createVerticalStrut(14));
        card.add(play);

        // ---- right box: the saved players
        buildSavedColumn();

        JPanel boxes = new JPanel();                 // the two boxes side by side, centred as a group
        boxes.setOpaque(false);
        boxes.setLayout(new BoxLayout(boxes, BoxLayout.X_AXIS));
        boxes.add(card);
        boxes.add(savedColumn);
        add(boxes);
    }

    // ------------------------------------------------------------ behaviour

    /**
     * Start: a name that belongs to a saved player loads that player (progress + achievements restored);
     * any other name creates a new player with a fresh save file. Blank = "Player".
     */
    private void go() {
        String name = nameField.getText().isBlank() ? DEFAULT_NAME : nameField.getText().trim();
        GameManager gm = app.getGameManager();
        try {
            GameManager.SavedPlayer existing = gm.listSavedPlayers().stream()
                    .filter(p -> p.username().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (existing != null)
                gm.loadPlayer(existing.username());
            else
                gm.createPlayer(name);          // saved right away, so it is listed next time the program starts
            error.setText(" ");
            app.show(ZooQuestGUI.MENU);
        } catch (IllegalArgumentException ex) { // e.g. name longer than 20 characters
            error.setText(ex.getMessage());
        } catch (IOException ex) {
            error.setText("Could not create the save file: " + ex.getMessage());
        }
    }

    /** Called every time this screen appears: reloads the saved players from the save folder. */
    @Override
    public void onShow() {
        model.clear();
        List<GameManager.SavedPlayer> players = app.getGameManager().listSavedPlayers();
        players.forEach(model::addElement);
        savedColumn.setVisible(!players.isEmpty());
        nameField.setText("");
        error.setText(" ");
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ saved-player box

    private void buildSavedColumn() {
        JPanel savedCard = ZooQuestGUI.card(20);                       // its own translucent box
        savedCard.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));

        JLabel heading = new JLabel("Or pick a saved player");
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("SansSerif", Font.PLAIN, 15));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setBackground(new Color(14, 28, 20));
        playerList.setFixedCellWidth(LIST_WIDTH - 24);   // rows fit the list: no sideways scrolling, long names are trimmed
        playerList.setCellRenderer(new PlayerRenderer());
        playerList.setSelectionBackground(new Color(46, 150, 80));
        playerList.addListSelectionListener(e -> {                      // clicking a player fills in the name
            GameManager.SavedPlayer chosen = playerList.getSelectedValue();
            if (chosen != null) {
                nameField.setText(chosen.username());
                error.setText(" ");
            }
        });
        playerList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && playerList.getSelectedValue() != null)
                    go();                                               // double-click = start as that player
            }
        });

        JScrollPane scroll = new JScrollPane(playerList);
        scroll.setBorder(BorderFactory.createLineBorder(ZooQuestGUI.GOLD, 1));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));
        scroll.setMaximumSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));
        scroll.setAlignmentX(CENTER_ALIGNMENT);

        savedCard.add(heading);
        savedCard.add(Box.createVerticalStrut(8));
        savedCard.add(scroll);

        savedColumn.setOpaque(false);
        savedColumn.setBorder(BorderFactory.createEmptyBorder(0, BOX_GAP, 0, 0));   // gap between the boxes
        savedColumn.add(savedCard, BorderLayout.CENTER);
        savedColumn.setVisible(false);
    }

    /** One row, everything centred: the player's name, with a small progress line underneath. */
    private static class PlayerRenderer extends JPanel implements ListCellRenderer<GameManager.SavedPlayer> {
        private final JLabel name = new JLabel("", SwingConstants.CENTER);

        PlayerRenderer() {
            super(new GridLayout(1, 1));
            name.setFont(new Font("SansSerif", Font.BOLD, 17));
            name.setForeground(Color.WHITE);
            add(name);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GameManager.SavedPlayer> list,
                GameManager.SavedPlayer p, int index, boolean selected, boolean focus) {
            name.setText(p.username());
            setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            return this;
        }
    }
}