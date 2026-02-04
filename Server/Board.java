import java.util.*;

public class Board {
    private final int boardW, boardH, noteW, noteH;
    private final Set<String> validColors;

    private final List<Note> notes = new ArrayList<>();
    private final List<Pin> pins = new ArrayList<>();

    public Board(int boardW, int boardH, int noteW, int noteH, String[] colors) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;

        validColors = new HashSet<>();
        for (String c : colors) validColors.add(c);
    }

    public synchronized String post(int x, int y, String color, String message) {
        if (message == null) message = "";

        if (x < 0 || y < 0 || x + noteW > boardW || y + noteH > boardH) {
            return "ERROR OUT_OF_BOUNDS Note exceeds board boundaries";
        }
        if (!validColors.contains(color)) {
            return "ERROR COLOR_NOT_SUPPORTED " + color + " is not a valid color";
        }

        Note newNote = new Note(x, y, noteW, noteH, color, message);

        // complete overlap = same rectangle (fixed note size => same x,y is enough)
        for (Note n : notes) {
            if (newNote.completelyOverlaps(n)) {
                return "ERROR COMPLETE_OVERLAP Note overlaps an existing note entirely";
            }
        }

        notes.add(newNote);
        return "OK NOTE_POSTED";
    }

    public synchronized List<Note> getNotesFiltered(String color, Integer containsX, Integer containsY, String refersTo) {
        List<Note> result = new ArrayList<>();
        for (Note n : notes) {
            if (color != null && !n.color.equals(color)) continue;

            if (containsX != null && containsY != null) {
                if (!n.contains(containsX, containsY)) continue;
            }

            if (refersTo != null && !refersTo.isEmpty()) {
                if (n.message == null) continue;
                if (!n.message.contains(refersTo)) continue;
            }

            result.add(n);
        }
        return result;
    }

    public synchronized String pin(int x, int y) {
        boolean insideAny = false;
        for (Note n : notes) {
            if (n.contains(x, y)) {
                insideAny = true;
                break;
            }
        }
        if (!insideAny) {
            return "ERROR NO_NOTE_AT_COORDINATE No note contains the given point";
        }

        for (Pin p : pins) {
            if (p.x == x && p.y == y) {
                // Pin already exists; still OK (idempotent)
                return "OK PIN_ADDED";
            }
        }

        pins.add(new Pin(x, y));
        return "OK PIN_ADDED";
    }

    public synchronized String unpin(int x, int y) {
        for (int i = 0; i < pins.size(); i++) {
            Pin p = pins.get(i);
            if (p.x == x && p.y == y) {
                pins.remove(i);
                return "OK PIN_REMOVED";
            }
        }
        return "ERROR PIN_NOT_FOUND No pin exists at the given coordinates";
    }

    public synchronized String shake() {
        // atomic: this whole method is synchronized
        List<Note> kept = new ArrayList<>();
        for (Note n : notes) {
            if (isPinned(n)) kept.add(n);
        }
        notes.clear();
        notes.addAll(kept);

        // remove pins that are no longer inside any note
        pins.removeIf(p -> !isInsideAnyNote(p.x, p.y));

        return "OK SHAKE_COMPLETE";
    }

    public synchronized String clear() {
        notes.clear();
        pins.clear();
        return "OK CLEARED";
    }

    public synchronized List<Pin> getPins() {
        return new ArrayList<>(pins);
    }

    public synchronized boolean isPinned(Note n) {
        for (Pin p : pins) {
            if (n.contains(p.x, p.y)) return true;
        }
        return false;
    }

    private boolean isInsideAnyNote(int x, int y) {
        for (Note n : notes) {
            if (n.contains(x, y)) return true;
        }
        return false;
    }
}
