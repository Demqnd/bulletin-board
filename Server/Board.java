import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Board {

    public static class PostRes {
        public final boolean ok;
        public final String code;
        public final String msg;

        private PostRes(boolean ok, String code, String msg) {
            this.ok = ok;
            this.code = code;
            this.msg = msg;
        }

        public static PostRes ok() {
            return new PostRes(true, "", "");
        }

        public static PostRes err(String code, String msg) {
            return new PostRes(false, code, msg);
        }
    }

    private final int bw, bh;
    private final int nw, nh;
    private final String[] cols;

    private final List<Note> notes = new ArrayList<>();
    private final List<Pin> pins = new ArrayList<>();

    public Board(int bw, int bh, int nw, int nh, String[] cols) {
        this.bw = bw;
        this.bh = bh;
        this.nw = nw;
        this.nh = nh;
        this.cols = cols;
    }

    public int getBoardW() { return bw; }
    public int getBoardH() { return bh; }
    public int getNoteW() { return nw; }
    public int getNoteH() { return nh; }

    public String colorsLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("COLORS ").append(cols.length);
        for (String c : cols) sb.append(" ").append(c);
        return sb.toString();
    }

    public boolean colorOk(String c) {
        for (String s : cols) {
            if (s.equals(c)) return true;
        }
        return false;
    }

    public synchronized void clear() {
        notes.clear();
        pins.clear();
    }

    public synchronized void shake() {
        List<Note> gone = new ArrayList<>();

        Iterator<Note> it = notes.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            if (!n.isPinned()) {
                gone.add(n);
                it.remove();
            }
        }

        if (gone.isEmpty()) return;

        Iterator<Pin> pit = pins.iterator();
        while (pit.hasNext()) {
            Pin p = pit.next();
            p.removeNotes(gone);
            if (p.isEmpty()) pit.remove();
        }
    }

    public synchronized boolean pinAt(int x, int y) {
        List<Note> hits = notesAt(x, y);
        if (hits.isEmpty()) return false;

        Pin p = findPin(x, y);
        if (p == null) {
            p = new Pin(x, y);
            pins.add(p);
        }

        for (Note n : hits) {
            p.addIfMissing(n);
        }
        return true;
    }

    public synchronized boolean unpinAt(int x, int y) {
        Pin p = findPin(x, y);
        if (p == null) return false;

        p.removeAll();
        pins.remove(p);
        return true;
    }

    public synchronized PostRes post(int x, int y, String col, String msg) {
        if (!colorOk(col)) {
            return PostRes.err("COLOR_NOT_SUPPORTED", col + " is not a valid color");
        }

        if (!fits(x, y)) {
            return PostRes.err("OUT_OF_BOUNDS", "Note exceeds board boundaries");
        }

        for (Note ex : notes) {
            if (ex.getX() == x && ex.getY() == y && ex.getW() == nw && ex.getH() == nh) {
                return PostRes.err("COMPLETE_OVERLAP", "Note overlaps an existing note entirely");
            }
        }

        Note n = new Note(x, y, nw, nh, col, msg);
        notes.add(n);

        for (Pin p : pins) {
            if (n.contains(p.getX(), p.getY())) {
                p.addIfMissing(n);
            }
        }

        return PostRes.ok();
    }

    public synchronized List<Note> getNotes(String col, Integer cx, Integer cy, String ref) {
        List<Note> out = new ArrayList<>();

        for (Note n : notes) {
            if (col != null && !n.getColor().equals(col)) continue;

            if (cx != null && cy != null) {
                if (!n.contains(cx, cy)) continue;
            }

            if (ref != null) {
                String a = n.getMsg().toLowerCase();
                String b = ref.toLowerCase();
                if (!a.contains(b)) continue;
            }

            out.add(n);
        }

        return out;
    }

    public synchronized List<Pin> pinsSnap() {
        return new ArrayList<>(pins);
    }

    private boolean fits(int x, int y) {
        if (x < 0 || y < 0) return false;
        return (x + nw) <= bw && (y + nh) <= bh;
    }

    private List<Note> notesAt(int x, int y) {
        List<Note> out = new ArrayList<>();
        for (Note n : notes) {
            if (n.contains(x, y)) out.add(n);
        }
        return out;
    }

    private Pin findPin(int x, int y) {
        for (Pin p : pins) {
            if (p.getX() == x && p.getY() == y) return p;
        }
        return null;
    }
}
