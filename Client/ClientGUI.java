import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ClientGUI extends JFrame {

    private final ClientConnection conn = new ClientConnection();

    private JTextField hostTf;
    private JTextField portTf;

    private JButton connectBtn;
    private JButton disconnectBtn;

    private JButton postBtn;
    private JButton getBtn;
    private JButton pinBtn;
    private JButton unpinBtn;
    private JButton shakeBtn;
    private JButton clearBtn;

    private JTextArea outTa;
    private JLabel statusLb;

    public ClientGUI() {
        super("BBoard Client");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 520);
        setLocationRelativeTo(null);

        build();
        setUiConnected(false);
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        root.add(topBar(), BorderLayout.NORTH);
        root.add(mainArea(), BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel topBar() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));

        left.add(new JLabel("Host"));
        hostTf = new JTextField("localhost", 14);
        left.add(hostTf);

        left.add(new JLabel("Port"));
        portTf = new JTextField("", 6);
        left.add(portTf);

        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());

        left.add(connectBtn);
        left.add(disconnectBtn);

        statusLb = new JLabel("Not connected");

        p.add(left, BorderLayout.WEST);
        p.add(statusLb, BorderLayout.EAST);

        return p;
    }

    private JPanel mainArea() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));

        postBtn = new JButton("POST");
        getBtn = new JButton("GET");
        pinBtn = new JButton("PIN");
        unpinBtn = new JButton("UNPIN");
        shakeBtn = new JButton("SHAKE");
        clearBtn = new JButton("CLEAR");

        postBtn.addActionListener(e -> postDialog());
        getBtn.addActionListener(e -> getDialog());
        pinBtn.addActionListener(e -> pinDialog());
        unpinBtn.addActionListener(e -> unpinDialog());
        shakeBtn.addActionListener(e -> sendCmd("SHAKE"));
        clearBtn.addActionListener(e -> sendCmd("CLEAR"));

        btns.add(postBtn);
        btns.add(getBtn);
        btns.add(pinBtn);
        btns.add(unpinBtn);
        btns.add(shakeBtn);
        btns.add(clearBtn);

        outTa = new JTextArea();
        outTa.setEditable(false);

        p.add(btns, BorderLayout.NORTH);
        p.add(new JScrollPane(outTa), BorderLayout.CENTER);

        return p;
    }

    private void setUiConnected(boolean on) {
        connectBtn.setEnabled(!on);
        disconnectBtn.setEnabled(on);

        postBtn.setEnabled(on);
        getBtn.setEnabled(on);
        pinBtn.setEnabled(on);
        unpinBtn.setEnabled(on);
        shakeBtn.setEnabled(on);
        clearBtn.setEnabled(on);

        statusLb.setText(on ? "Connected" : "Not connected");
    }

    private void setBusy(boolean busy) {
        if (conn.connected()) {
            postBtn.setEnabled(!busy);
            getBtn.setEnabled(!busy);
            pinBtn.setEnabled(!busy);
            unpinBtn.setEnabled(!busy);
            shakeBtn.setEnabled(!busy);
            clearBtn.setEnabled(!busy);
            disconnectBtn.setEnabled(!busy);
        }
        connectBtn.setEnabled(!busy && !conn.connected());
    }

    private void log(String s) {
        outTa.append(s);
        outTa.append("\n");
        outTa.setCaretPosition(outTa.getDocument().getLength());
    }

    private void connect() {
        String host = hostTf.getText().trim();
        int port;

        try {
            port = Integer.parseInt(portTf.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port must be an integer", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setBusy(true);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                conn.connect(host, port);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setUiConnected(true);

                    log("Connected.");
                    log("BOARD " + conn.bw() + " " + conn.bh());
                    log("NOTE " + conn.nw() + " " + conn.nh());
                    log("COLORS " + conn.colors());

                } catch (Exception ex) {
                    conn.close();
                    setUiConnected(false);
                    log("Connect failed: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ClientGUI.this, "Connect failed:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        };

        w.execute();
    }

    private void disconnect() {
        if (!conn.connected()) {
            setUiConnected(false);
            return;
        }

        setBusy(true);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try { conn.send("DISCONNECT"); } catch (Exception e) { }
                conn.close();
                return null;
            }

            @Override
            protected void done() {
                setUiConnected(false);
                log("Disconnected.");
                setBusy(false);
            }
        };

        w.execute();
    }

    private void sendCmd(String cmd) {
        if (!conn.connected()) return;

        setBusy(true);

        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return conn.send(cmd);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    log("> " + cmd);
                    log(resp);
                } catch (Exception ex) {
                    log("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ClientGUI.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        };

        w.execute();
    }

    private void pinDialog() {
        XY p = askXY("PIN");
        if (p == null) return;
        sendCmd("PIN " + p.x + " " + p.y);
    }

    private void unpinDialog() {
        XY p = askXY("UNPIN");
        if (p == null) return;
        sendCmd("UNPIN " + p.x + " " + p.y);
    }

    private XY askXY(String title) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));

        JTextField xTf = new JTextField();
        JTextField yTf = new JTextField();

        panel.add(new JLabel("x"));
        panel.add(xTf);
        panel.add(new JLabel("y"));
        panel.add(yTf);

        int r = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return null;

        Integer x = toInt(xTf.getText().trim());
        Integer y = toInt(yTf.getText().trim());

        if (x == null || y == null) {
            JOptionPane.showMessageDialog(this, "x and y must be integers", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return new XY(x, y);
    }

    private void postDialog() {
        if (!conn.connected()) return;

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));

        JTextField xTf = new JTextField();
        JTextField yTf = new JTextField();
        JTextField msgTf = new JTextField();

        List<String> list = conn.colors();
        JComboBox<String> colBox = new JComboBox<>(list.toArray(new String[0]));

        panel.add(new JLabel("x"));
        panel.add(xTf);
        panel.add(new JLabel("y"));
        panel.add(yTf);
        panel.add(new JLabel("color"));
        panel.add(colBox);
        panel.add(new JLabel("message"));
        panel.add(msgTf);

        int r = JOptionPane.showConfirmDialog(this, panel, "POST", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        Integer x = toInt(xTf.getText().trim());
        Integer y = toInt(yTf.getText().trim());
        if (x == null || y == null) {
            JOptionPane.showMessageDialog(this, "x and y must be integers", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String col = (String) colBox.getSelectedItem();
        String msg = msgTf.getText();

        if (col == null || col.trim().isEmpty() || msg.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Color/message must not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sendCmd("POST " + x + " " + y + " " + col + " " + msg);
    }

    private void getDialog() {
        if (!conn.connected()) return;

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));

        JComboBox<String> colBox = new JComboBox<>();
        colBox.addItem("(any)");
        for (String c : conn.colors()) colBox.addItem(c);

        JTextField cxTf = new JTextField();
        JTextField cyTf = new JTextField();
        JTextField refTf = new JTextField();

        panel.add(new JLabel("color"));
        panel.add(colBox);

        panel.add(new JLabel("contains x (opt)"));
        panel.add(cxTf);
        panel.add(new JLabel("contains y (opt)"));
        panel.add(cyTf);

        panel.add(new JLabel("refersTo (opt)"));
        panel.add(refTf);

        int r = JOptionPane.showConfirmDialog(this, panel, "GET", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        StringBuilder cmd = new StringBuilder("GET");

        String picked = (String) colBox.getSelectedItem();
        if (picked != null && !picked.equals("(any)")) {
            cmd.append(" color=").append(picked);
        }

        String cx = cxTf.getText().trim();
        String cy = cyTf.getText().trim();
        if (!cx.isEmpty() || !cy.isEmpty()) {
            Integer x = toInt(cx);
            Integer y = toInt(cy);
            if (x == null || y == null) {
                JOptionPane.showMessageDialog(this, "contains needs both ints (or both empty)",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            cmd.append(" contains=").append(x).append(" ").append(y);
        }

        String ref = refTf.getText().trim();
        if (!ref.isEmpty()) {
            if (ref.contains(" ")) {
                JOptionPane.showMessageDialog(this, "refersTo (basic GUI) can't include spaces",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            cmd.append(" refersTo=").append(ref);
        }

        sendCmd(cmd.toString());
    }

    private Integer toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static class XY {
        final int x, y;
        XY(int x, int y) { this.x = x; this.y = y; }
    }
}
