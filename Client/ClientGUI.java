import javax.swing.*;
import java.awt.*;

public class ClientGUI extends JFrame {

    // --- Connection UI ---
    private final JTextField hostField = new JTextField("127.0.0.1", 12);
    private final JTextField portField = new JTextField("4554", 6);
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");

    // --- POST UI ---
    private final JTextField xField = new JTextField("10", 4);
    private final JTextField yField = new JTextField("10", 4);
    private final JTextField colorField = new JTextField("red", 8);
    private final JTextField msgField = new JTextField("Hello", 22);
    private final JButton postBtn = new JButton("POST");

    // --- Raw command UI ---
    private final JTextField rawField = new JTextField("GET", 30);
    private final JButton sendBtn = new JButton("Send Raw");
    private final JButton refreshBtn = new JButton("Refresh");

    // --- Visual board ---
    private final BoardPanel boardPanel = new BoardPanel();

    // --- Output log ---
    private final JTextArea output = new JTextArea(12, 70);

    // --- Networking ---
    private final ClientConnection conn = new ClientConnection();

    public ClientGUI() {
        super("Bulletin Board Client");

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);

        setLayout(new BorderLayout());

        // NORTH: controls stacked
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(makeConnectRow());
        north.add(makePostRow());
        north.add(makeRawRow());
        add(north, BorderLayout.NORTH);

        // CENTER: board drawing panel
        add(boardPanel, BorderLayout.CENTER);

        // SOUTH: output log
        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createTitledBorder("Client Log"));
        add(scroll, BorderLayout.SOUTH);

        // Initial button states
        disconnectBtn.setEnabled(false);
        postBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        refreshBtn.setEnabled(false);

        // Actions
        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn.addActionListener(e -> doDisconnect());
        postBtn.addActionListener(e -> doPost());
        sendBtn.addActionListener(e -> doSendRaw());
        refreshBtn.addActionListener(e -> refreshVisuals());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel makeConnectRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("Host:"));
        p.add(hostField);
        p.add(new JLabel("Port:"));
        p.add(portField);
        p.add(connectBtn);
        p.add(disconnectBtn);
        return p;
    }

    private JPanel makePostRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createTitledBorder("POST"));
        p.add(new JLabel("x:"));
        p.add(xField);
        p.add(new JLabel("y:"));
        p.add(yField);
        p.add(new JLabel("color:"));
        p.add(colorField);
        p.add(new JLabel("msg:"));
        p.add(msgField);
        p.add(postBtn);
        return p;
    }

    private JPanel makeRawRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createTitledBorder("Raw Command"));
        p.add(rawField);
        p.add(sendBtn);
        p.add(refreshBtn);
        return p;
    }

    private void doConnect() {
        String host = hostField.getText().trim();
        int port;

        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            log("Port must be an integer.");
            return;
        }

        connectBtn.setEnabled(false);
        log("Connecting to " + host + ":" + port + "...");

        new Thread(() -> {
            try {
                conn.connect(host, port);

                SwingUtilities.invokeLater(() -> {
                    log("Connected.");
                    log("Handshake:");
                    log("  BOARD " + conn.boardW + " " + conn.boardH);
                    log("  NOTE " + conn.noteW + " " + conn.noteH);
                    log("  COLORS " + String.join(" ", conn.colors));

                    // Configure the visual board
                    boardPanel.setConfig(conn.boardW, conn.boardH, conn.noteW, conn.noteH);

                    disconnectBtn.setEnabled(true);
                    postBtn.setEnabled(true);
                    sendBtn.setEnabled(true);
                    refreshBtn.setEnabled(true);

                    refreshVisuals();
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    log("Connect failed: " + ex.getMessage());
                    connectBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void doDisconnect() {
        conn.disconnect();
        log("Disconnected.");

        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        postBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        refreshBtn.setEnabled(false);

        // Clear visuals
        boardPanel.setState(new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    private void doPost() {
        if (!conn.isConnected()) {
            log("Not connected.");
            return;
        }

        int x, y;
        try {
            x = Integer.parseInt(xField.getText().trim());
            y = Integer.parseInt(yField.getText().trim());
        } catch (NumberFormatException ex) {
            log("POST: x and y must be integers.");
            return;
        }

        String color = colorField.getText().trim();
        String msg = msgField.getText(); // keep spaces

        String cmd = "POST " + x + " " + y + " " + color + " " + msg;
        sendAndMaybeRefresh(cmd, true);
    }

    private void doSendRaw() {
        if (!conn.isConnected()) {
            log("Not connected.");
            return;
        }
        String cmd = rawField.getText();
        // For raw, we refresh too (helps for PIN/UNPIN/SHAKE/CLEAR/GET)
        sendAndMaybeRefresh(cmd, true);
    }

    private void sendAndMaybeRefresh(String cmd, boolean refreshAfter) {
        log(">> " + cmd);

        new Thread(() -> {
            try {
                String resp = conn.sendCommand(cmd);

                SwingUtilities.invokeLater(() -> {
                    // log response (supports multiline)
                    for (String line : resp.split("\\R")) {
                        log("<< " + line);
                    }
                });

                if (refreshAfter) {
                    // If the command changes state, refresh the drawings.
                    // Even if it doesn't, refresh is cheap.
                    refreshVisuals();
                }

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> log("Command failed: " + ex.getMessage()));
            }
        }).start();
    }

    private void refreshVisuals() {
        if (!conn.isConnected()) return;

        new Thread(() -> {
            try {
                String notesResp = conn.sendCommand("GET");
                String pinsResp = conn.sendCommand("GET PINS");

                java.util.List<BoardPanel.NoteView> notes = parseNotes(notesResp);
                java.util.List<BoardPanel.PinView> pins = parsePins(pinsResp);

                SwingUtilities.invokeLater(() -> boardPanel.setState(notes, pins));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> log("Refresh failed: " + ex.getMessage()));
            }
        }).start();
    }

    // Expects your server format from the screenshot:
    // OK N
    // NOTE x y color message PINNED=false
    private java.util.List<BoardPanel.NoteView> parseNotes(String resp) {
        java.util.List<BoardPanel.NoteView> out = new java.util.ArrayList<>();
        if (resp == null || resp.isEmpty()) return out;

        String[] lines = resp.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("NOTE ")) continue;

            // NOTE x y color rest...
            String[] parts = line.split(" ", 5);
            if (parts.length < 5) continue;

            int x, y;
            try {
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }

            String colorName = parts[3];
            String rest = parts[4]; // message + " PINNED=..."

            boolean pinned = false;
            String msg = rest;

            int pinIdx = rest.lastIndexOf(" PINNED=");
            if (pinIdx >= 0) {
                msg = rest.substring(0, pinIdx);
                String p = rest.substring(pinIdx + " PINNED=".length()).trim();
                pinned = p.equalsIgnoreCase("true");
            }

            Color awt = mapColor(colorName);
            out.add(new BoardPanel.NoteView(x, y, msg, awt, pinned));
        }
        return out;
    }

    // Expects:
    // OK N
    // PIN x y
    private java.util.List<BoardPanel.PinView> parsePins(String resp) {
        java.util.List<BoardPanel.PinView> out = new java.util.ArrayList<>();
        if (resp == null || resp.isEmpty()) return out;

        String[] lines = resp.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("PIN ")) continue;

            String[] parts = line.split("\\s+");
            if (parts.length != 3) continue;

            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                out.add(new BoardPanel.PinView(x, y));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private Color mapColor(String name) {
        if (name == null) return new Color(255, 240, 140);
        return switch (name.toLowerCase()) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            case "orange" -> Color.ORANGE;
            case "pink" -> Color.PINK;
            case "cyan" -> Color.CYAN;
            case "gray", "grey" -> Color.GRAY;
            default -> new Color(255, 240, 140); // sticky-note fallback
        };
    }

    private void log(String s) {
        output.append(s + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }
}
