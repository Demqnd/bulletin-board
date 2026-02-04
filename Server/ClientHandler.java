import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final Board board;

    private final int boardW, boardH, noteW, noteH;
    private final String[] colors;

    public ClientHandler(Socket socket, Board board, int boardW, int boardH, int noteW, int noteH, String[] colors) {
        this.socket = socket;
        this.board = board;
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;
        this.colors = colors;
    }

    @Override
    public void run() {
        try (
            Socket s = socket;
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            // Handshake
            out.write("BOARD " + boardW + " " + boardH + "\n");
            out.write("NOTE " + noteW + " " + noteH + "\n");
            out.write("COLORS " + String.join(" ", colors) + "\n");
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // DISCONNECT
                if (line.equalsIgnoreCase("DISCONNECT")) {
                    out.write("OK DISCONNECTED\n");
                    out.flush();
                    break;
                }

                // CLEAR
                if (line.equalsIgnoreCase("CLEAR")) {
                    out.write(board.clear() + "\n");
                    out.flush();
                    continue;
                }

                // SHAKE
                if (line.equalsIgnoreCase("SHAKE")) {
                    out.write(board.shake() + "\n");
                    out.flush();
                    continue;
                }

                // GET PINS
                if (line.equalsIgnoreCase("GET PINS")) {
                    List<Pin> pins = board.getPins();
                    out.write("OK " + pins.size() + "\n");
                    for (Pin p : pins) {
                        out.write("PIN " + p.x + " " + p.y + "\n");
                    }
                    out.flush();
                    continue;
                }

                // GET (with optional filters)
                if (line.equalsIgnoreCase("GET") || line.toUpperCase().startsWith("GET ")) {
                    handleGet(line, out);
                    continue;
                }

                // POST
                if (line.toUpperCase().startsWith("POST ")) {
                    String resp = handlePost(line);
                    out.write(resp + "\n");
                    out.flush();
                    continue;
                }

                // PIN
                if (line.toUpperCase().startsWith("PIN ")) {
                    String resp = handlePin(line);
                    out.write(resp + "\n");
                    out.flush();
                    continue;
                }

                // UNPIN
                if (line.toUpperCase().startsWith("UNPIN ")) {
                    String resp = handleUnpin(line);
                    out.write(resp + "\n");
                    out.flush();
                    continue;
                }

                out.write("ERROR INVALID_FORMAT Unknown command\n");
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("Client handler ended: " + e.getMessage());
        }
    }

    private String handlePost(String line) {
        // POST <x> <y> <color> <message...>
        String[] parts = line.split(" ", 5);
        if (parts.length < 5) {
            return "ERROR INVALID_FORMAT POST requires coordinates, color, and message";
        }
        int x, y;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT Coordinates must be integers";
        }
        String color = parts[3];
        String msg = parts[4];
        return board.post(x, y, color, msg);
    }

    private String handlePin(String line) {
        // PIN <x> <y>
        String[] parts = line.split("\\s+");
        if (parts.length != 3) return "ERROR INVALID_FORMAT PIN requires x y";
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            return board.pin(x, y);
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT Coordinates must be integers";
        }
    }

    private String handleUnpin(String line) {
        // UNPIN <x> <y>
        String[] parts = line.split("\\s+");
        if (parts.length != 3) return "ERROR INVALID_FORMAT UNPIN requires x y";
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            return board.unpin(x, y);
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT Coordinates must be integers";
        }
    }

    private void handleGet(String line, BufferedWriter out) throws IOException {
        // GET [color=<c>] [contains=<x> <y>] [refersTo=<substring>]
        String upper = line.toUpperCase();

        String refersTo = null;
        int idx = upper.indexOf("REFERS_TO=");
        if (idx >= 0) idx = upper.indexOf("REFERSTO="); // tolerate both? (but spec uses refersTo)
        idx = upper.indexOf("REFERSTO=");
        if (idx >= 0) {
            refersTo = line.substring(idx + "refersTo=".length()).trim();
            line = line.substring(0, idx).trim();
        }

        String color = null;
        Integer cx = null, cy = null;

        String[] tokens = line.split("\\s+");
        // tokens[0] is GET
        for (int i = 1; i < tokens.length; i++) {
            String t = tokens[i];

            if (t.startsWith("color=")) {
                color = t.substring("color=".length());
            } else if (t.startsWith("contains=")) {
                String after = t.substring("contains=".length());
                try {
                    cx = Integer.parseInt(after);
                } catch (NumberFormatException e) {
                    // maybe "contains=" with no number
                    returnError(out, "INVALID_FORMAT", "contains requires two integers");
                    return;
                }
                if (i + 1 >= tokens.length) {
                    returnError(out, "INVALID_FORMAT", "contains requires two integers");
                    return;
                }
                try {
                    cy = Integer.parseInt(tokens[++i]);
                } catch (NumberFormatException e) {
                    returnError(out, "INVALID_FORMAT", "contains requires two integers");
                    return;
                }
            } else if (t.startsWith("refersTo=")) {
                // allow refersTo to appear without spaces in substring
                refersTo = t.substring("refersTo=".length());
            }
        }

        List<Note> notes = board.getNotesFiltered(color, cx, cy, refersTo);

        out.write("OK " + notes.size() + "\n");
        for (Note n : notes) {
            boolean pinned = board.isPinned(n);
            out.write("NOTE " + n.x + " " + n.y + " " + n.color + " " + n.message + " PINNED=" + pinned + "\n");
        }
        out.flush();
    }

    private void returnError(BufferedWriter out, String code, String msg) throws IOException {
        out.write("ERROR " + code + " " + msg + "\n");
        out.flush();
    }
}
