import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientConnection {

    private Socket sock;
    private BufferedReader in;
    private PrintWriter out;

    private int bw, bh, nw, nh;
    private final List<String> cols = new ArrayList<>();

    public void connect(String host, int port) throws IOException {
        close();

        sock = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);

        String a = in.readLine();
        String b = in.readLine();
        String c = in.readLine();

        if (a == null || b == null || c == null) {
            throw new IOException("Handshake failed");
        }

        readHandshake(a, b, c);
    }

    public boolean connected() {
        return sock != null && sock.isConnected() && !sock.isClosed();
    }

    public void close() {
        try { if (sock != null) sock.close(); } catch (IOException e) { }

        sock = null;
        in = null;
        out = null;

        bw = bh = nw = nh = 0;
        cols.clear();
    }

    public String send(String cmd) throws IOException {
        if (!connected() || in == null || out == null) {
            throw new IOException("Not connected");
        }

        out.println(cmd);

        String first = in.readLine();
        if (first == null) throw new IOException("Server closed connection");

        String t = cmd.trim();
        if (t.startsWith("GET")) {
            int n = okCount(first);
            if (n >= 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(first);
                for (int i = 0; i < n; i++) {
                    String next = in.readLine();
                    if (next == null) throw new IOException("Server closed connection mid-response");
                    sb.append("\n").append(next);
                }
                return sb.toString();
            }
        }

        return first;
    }

    public int bw() { return bw; }
    public int bh() { return bh; }
    public int nw() { return nw; }
    public int nh() { return nh; }

    public List<String> colors() {
        return new ArrayList<>(cols);
    }

    private void readHandshake(String boardLine, String noteLine, String colorsLine) throws IOException {
        String[] b = boardLine.trim().split("\\s+");
        String[] n = noteLine.trim().split("\\s+");
        String[] c = colorsLine.trim().split("\\s+");

        if (b.length != 3 || !b[0].equals("BOARD")) throw new IOException("Bad handshake: " + boardLine);
        if (n.length != 3 || !n[0].equals("NOTE")) throw new IOException("Bad handshake: " + noteLine);
        if (c.length < 2 || !c[0].equals("COLORS")) throw new IOException("Bad handshake: " + colorsLine);

        bw = mustInt(b[1], "BOARD w");
        bh = mustInt(b[2], "BOARD h");
        nw = mustInt(n[1], "NOTE w");
        nh = mustInt(n[2], "NOTE h");

        int k = mustInt(c[1], "COLORS count");
        cols.clear();

        if (k < 0) throw new IOException("Bad COLORS count");
        if (c.length != 2 + k) throw new IOException("Bad COLORS list");

        for (int i = 0; i < k; i++) cols.add(c[2 + i]);
    }

    private int mustInt(String s, String what) throws IOException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IOException("Bad int for " + what + ": " + s);
        }
    }

    private int okCount(String firstLine) {
        String[] p = firstLine.trim().split("\\s+");
        if (p.length != 2) return -1;
        if (!p[0].equals("OK")) return -1;
        try {
            return Integer.parseInt(p[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
