package main.UI;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import main.GameManager;

/*
- START screen: "Enter Your Name" + text field + START (layout PDF, page 1), 
plus a second box that lists every saved player.

- This screen used to call initializeGame(name) only. That builds the player in memory and never
writes a save file, so a new player was lost as soon as the program closed, and nothing listed the saved
players. Now:
   - a name that belongs to a saved player loads that player (progress and badges restored);
   - any other name creates a new player and writes their save file straight away;
   - onShow() reloads the list of saved players every time the screen appears.
*/
public class StartPanel extends BackgroundPanel implements ZooQuestGUI.Refreshable {
    private static final String DEFAULT_NAME = "Player";
    private static final int LIST_WIDTH = 250;   
    private static final int LIST_HEIGHT = 150;  // about three players visible, the rest scrolls
    private static final int BOX_GAP = 24;       // space between the two boxes

    private final ZooQuestGUI app;
    private final JTextField nameField = new JTextField(16);
    private final JLabel error = new JLabel(" ", SwingConstants.CENTER);

    // the saved-player box lives in its own panel, so it (and the gap) takes no space while empty
    private final DefaultListModel<GameManager.SavedPlayer> model = new DefaultListModel<>();
    private final JList<GameManager.SavedPlayer> playerList = new JList<>(model);
    private final JPanel savedColumn = new JPanel(new BorderLayout());

    public StartPanel(ZooQuestGUI app) {
        super("main_screen_background.png");
        setFadeEnabled(true);                          // fade in / out between screens
        this.app = app; 
        JPanel card = ZooQuestGUI.card(26);

        JLabel prompt = new JLabel("Enter Your Name");
        prompt.setForeground(Color.WHITE);
        prompt.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 22));
        prompt.setAlignmentX(CENTER_ALIGNMENT);

        nameField.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 22));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setMaximumSize(new Dimension(320, 46));
        nameField.setPreferredSize(new Dimension(320, 46));
        nameField.setAlignmentX(CENTER_ALIGNMENT);

        error.setForeground(new Color(255, 140, 120));
        error.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 14));
        error.setAlignmentX(CENTER_ALIGNMENT);

        GuiKit.Btn start = new GuiKit.Btn("START", GuiKit.GREEN, 260, 48, 20);
        start.setAlignmentX(CENTER_ALIGNMENT);
        start.addActionListener(e -> go());
        nameField.addActionListener(e -> go());

        card.add(prompt);
        card.add(Box.createVerticalStrut(14));
        card.add(nameField);
        card.add(Box.createVerticalStrut(4));
        card.add(error);
        card.add(Box.createVerticalStrut(10));
        card.add(start);

        buildSavedColumn();

        JPanel boxes = new JPanel(); // the two boxes side by side, centred as a group
        boxes.setOpaque(false);
        boxes.setLayout(new BoxLayout(boxes, BoxLayout.X_AXIS));
        boxes.add(card);
        boxes.add(savedColumn);
        add(boxes);
    }

    // ------------------------------------------------------------ behaviour

    /*
    - START: a name that belongs to a saved player loads that player; any other name creates a new player
    with a fresh save file. Blank = "Player".
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
                gm.createPlayer(name);          // written to src/saves right away, so it is listed next time
            error.setText(" ");
            app.show(ZooQuestGUI.MENU);
        } catch (IllegalArgumentException ex) { // e.g. a name longer than 20 characters
            error.setText(ex.getMessage());
        } catch (IOException ex) {
            error.setText("Could not create the save file: " + ex.getMessage());
        }
    }

    // Called every time this screen appears: reloads the saved players from the save folder.
    @Override
    public void onShow() {
        model.clear();
        List<GameManager.SavedPlayer> players = app.getGameManager().listSavedPlayers();
        players.forEach(model::addElement);
        savedColumn.setVisible(!players.isEmpty());
        playerList.clearSelection();
        nameField.setText("");
        error.setText(" ");
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ saved-player box 

    private void buildSavedColumn() {
        JPanel savedCard = ZooQuestGUI.card(20);
        savedCard.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));

        JLabel heading = new JLabel("Or pick a saved player");
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 16));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setBackground(new Color(14, 28, 20));
        playerList.setFixedCellWidth(LIST_WIDTH - 24);   // rows fit the list: no sideways scrolling
        playerList.setCellRenderer(new PlayerRenderer());
        playerList.setSelectionBackground(new Color(46, 150, 80));
        playerList.addListSelectionListener(e -> {       // clicking a player fills in the name
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
                    go();                                // double-click = start as that player
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
        savedColumn.setBorder(BorderFactory.createEmptyBorder(0, BOX_GAP, 0, 0));
        savedColumn.add(savedCard, BorderLayout.CENTER);
        savedColumn.setVisible(false);
    }

    // One row, everything centred: the player's name, with a small progress line underneath.
    private static class PlayerRenderer extends JPanel implements ListCellRenderer<GameManager.SavedPlayer> {
        private final JLabel name = new JLabel("", SwingConstants.CENTER);
        private final JLabel summary = new JLabel("", SwingConstants.CENTER);

        PlayerRenderer() {
            super(new GridLayout(2, 1));
            name.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.BOLD, 17));
            name.setForeground(Color.WHITE);
            summary.setFont(new Font(ZooQuestGUI.FONT_NAME, Font.PLAIN, 13));
            summary.setForeground(new Color(200, 225, 205));
            add(name);
            add(summary);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GameManager.SavedPlayer> list,
                GameManager.SavedPlayer p, int index, boolean selected, boolean focus) {
            name.setText(p.username());
            summary.setText(p.completedBiomes() + "/" + GameManager.BIOME_COUNT + " biomes  |  "
                    + p.badges() + (p.badges() == 1 ? " badge" : " badges"));
            setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            return this;
        }
    }
}
