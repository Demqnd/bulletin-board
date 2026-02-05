import java.util.List;

public class Protocol {

    private static final String OK_DIS = "OK DISCONNECTED";
    private static final String OK_CLR = "OK CLEARED";
    private static final String OK_SHK = "OK SHAKEN";
    private static final String OK_PIN = "OK PINNED";
    private static final String OK_UNP = "OK UNPINNED";
    private static final String OK_POST = "OK NOTE_POSTED";

    private static final String E_FMT = "ERROR INVALID_FORMAT";
    private static final String E_OOB = "ERROR OUT_OF_BOUNDS";
    private static final String E_COL = "ERROR COLOR_NOT_SUPPORTED";
    private static final String E_OVR = "ERROR COMPLETE_OVERLAP";
    private static final String E_NON = "ERROR NO_NOTE_AT_COORDINATE";
    private static final String E_PNF = "ERROR PIN_NOT_FOUND";

    public static String handle(String line, Board board) {
        if (line == null) return err(E_FMT, "Invalid request");

        String t = line.trim();
        if (t.isEmpty()) return err(E_FMT, "Invalid request");

        String[] parts = t.split("\\s+");
        String cmd = parts[0];

        if (cmd.equals("DISCONNECT")) {
            if (parts.length != 1) return err(E_FMT, "DISCONNECT takes no parameters");
            return OK_DIS;
        }

        if (cmd.equals("CLEAR")) {
            if (parts.length != 1) return err(E_FMT, "CLEAR takes no parameters");
            board.clear();
            return OK_CLR;
        }

        if (cmd.equals("SHAKE")) {
            if (parts.length != 1) return err(E_FMT, "SHAKE takes no parameters");
            board.shake();
            return OK_SHK;
        }

        if (cmd.equals("PIN")) {
            if (parts.length != 3) return err(E_FMT, "PIN requires x and y");
            Integer x = toInt(parts[1]);
            Integer y = toInt(parts[2]);
            if (x == null || y == null) return err(E_FMT, "PIN requires integer coordinates");

            if (!board.pinAt(x, y)) return err(E_NON, "No note contains the given point");
            return OK_PIN;
        }

        if (cmd.equals("UNPIN")) {
            if (parts.length != 3) return err(E_FMT, "UNPIN requires x and y");
            Integer x = toInt(parts[1]);
            Integer y = toInt(parts[2]);
            if (x == null || y == null) return err(E_FMT, "UNPIN requires integer coordinates");

            if (!board.unpinAt(x, y)) return err(E_PNF, "No pin exists at the given coordinates");
            return OK_UNP;
        }

        if (cmd.equals("POST")) {
            return handlePost(t, board);
        }

        if (cmd.equals("GET")) {
            return handleGet(parts, board);
        }

        return err(E_FMT, "Unknown command");
    }

    public static boolean shouldClose(String resp) {
        return OK_DIS.equals(resp);
    }

    private static String handlePost(String full, Board board) {
        // POST <x> <y> <color> <message...>
        String[] a = full.split("\\s+", 5);
        if (a.length < 5) return err(E_FMT, "POST requires coordinates, color, and message");

        Integer x = toInt(a[1]);
        Integer y = toInt(a[2]);
        if (x == null || y == null) return err(E_FMT, "POST requires integer coordinates");

        String col = a[3];
        String msg = a[4];
        if (msg.trim().isEmpty()) return err(E_FMT, "POST requires coordinates, color, and message");

        Board.PostRes r = board.post(x, y, col, msg);
        if (r.ok) return OK_POST;

        if (r.code.equals("OUT_OF_BOUNDS")) return err(E_OOB, r.msg);
        if (r.code.equals("COLOR_NOT_SUPPORTED")) return err(E_COL, r.msg);
        if (r.code.equals("COMPLETE_OVERLAP")) return err(E_OVR, r.msg);

        return err("ERROR " + r.code, r.msg);
    }

    private static String handleGet(String[] parts, Board board) {
        // GET PINS
        if (parts.length == 2 && parts[1].equals("PINS")) {
            List<Pin> pins = board.pinsSnap();
            StringBuilder sb = new StringBuilder();
            sb.append("OK ").append(pins.size());
            for (Pin p : pins) {
                sb.append("\nPIN ").append(p.getX()).append(" ").append(p.getY());
            }
            return sb.toString();
        }

        String col = null;
        Integer cx = null, cy = null;
        String ref = null;

        int i = 1;
        while (i < parts.length) {
            String tok = parts[i];

            if (tok.startsWith("color=")) {
                if (col != null) return err(E_FMT, "Duplicate color filter");
                col = tok.substring("color=".length());
                if (col.isEmpty()) return err(E_FMT, "color filter must not be empty");
                i++;
                continue;
            }

            if (tok.startsWith("contains=")) {
                if (cx != null || cy != null) return err(E_FMT, "Duplicate contains filter");

                String xStr = tok.substring("contains=".length());
                if (xStr.isEmpty()) return err(E_FMT, "contains requires x and y");
                if (i + 1 >= parts.length) return err(E_FMT, "contains requires x and y");

                Integer x = toInt(xStr);
                Integer y = toInt(parts[i + 1]);
                if (x == null || y == null) return err(E_FMT, "contains requires integer coordinates");

                cx = x;
                cy = y;
                i += 2;
                continue;
            }

            if (tok.startsWith("refersTo=")) {
                if (ref != null) return err(E_FMT, "Duplicate refersTo filter");
                ref = tok.substring("refersTo=".length());
                if (ref.isEmpty()) return err(E_FMT, "refersTo filter must not be empty");
                i++;
                continue;
            }

            return err(E_FMT, "Unknown filter: " + tok);
        }

        if (col != null && !board.colorOk(col)) {
            return err(E_COL, col + " is not a valid color");
        }

        List<Note> hits = board.getNotes(col, cx, cy, ref);

        StringBuilder sb = new StringBuilder();
        sb.append("OK ").append(hits.size());
        for (Note n : hits) {
            sb.append("\nNOTE ")
              .append(n.getX()).append(" ")
              .append(n.getY()).append(" ")
              .append(n.getColor()).append(" ")
              .append(n.getMsg())
              .append(" PINNED=").append(n.isPinned() ? "true" : "false");
        }
        return sb.toString();
    }

    private static Integer toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String err(String code, String msg) {
        return code + " " + msg;
    }
}
