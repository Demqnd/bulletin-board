import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientConnection {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public int boardW, boardH, noteW, noteH;
    public String[] colors;

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        String boardLine = in.readLine();
        String noteLine = in.readLine();
        String colorsLine = in.readLine();
        if (boardLine == null || noteLine == null || colorsLine == null) {
            throw new IOException("Server closed during handshake");
        }

        parseHandshake(boardLine, noteLine, colorsLine);
    }

    private void parseHandshake(String boardLine, String noteLine, String colorsLine) throws IOException {
        String[] b = boardLine.trim().split("\\s+");
        if (b.length != 3 || !b[0].equals("BOARD")) throw new IOException("Bad handshake: " + boardLine);
        boardW = Integer.parseInt(b[1]);
        boardH = Integer.parseInt(b[2]);

        String[] n = noteLine.trim().split("\\s+");
        if (n.length != 3 || !n[0].equals("NOTE")) throw new IOException("Bad handshake: " + noteLine);
        noteW = Integer.parseInt(n[1]);
        noteH = Integer.parseInt(n[2]);

        String[] c = colorsLine.trim().split("\\s+");
        if (c.length < 2 || !c[0].equals("COLORS")) throw new IOException("Bad handshake: " + colorsLine);
        colors = new String[c.length - 1];
        System.arraycopy(c, 1, colors, 0, colors.length);
    }

    // Reads either:
    // - a single-line response (OK NOTE_POSTED / ERROR ...)
    // - or a multi-line response starting with "OK <N>" followed by N lines
    public synchronized String sendCommand(String cmd) throws IOException {
        if (!isConnected()) throw new IOException("Not connected");

        out.write(cmd);
        if (!cmd.endsWith("\n")) out.write("\n");
        out.flush();

        String first = in.readLine();
        if (first == null) throw new IOException("Server closed connection");

        String[] parts = first.trim().split("\\s+");
        if (parts.length == 2 && parts[0].equals("OK")) {
            try {
                int n = Integer.parseInt(parts[1]);
                StringBuilder sb = new StringBuilder();
                sb.append(first).append("\n");
                for (int i = 0; i < n; i++) {
                    String line = in.readLine();
                    if (line == null) throw new IOException("Server closed during multi-line response");
                    sb.append(line).append("\n");
                }
                return sb.toString().trim();
            } catch (NumberFormatException ignored) {
                // OK <not-a-number> => single line
            }
        }

        return first;
    }

    public void disconnect() {
        try {
            if (isConnected()) {
                try { sendCommand("DISCONNECT"); } catch (Exception ignored) {}
                socket.close();
            }
        } catch (IOException ignored) {
        } finally {
            socket = null;
            in = null;
            out = null;
        }
    }
}
