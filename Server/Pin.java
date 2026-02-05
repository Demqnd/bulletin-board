import java.util.ArrayList;
import java.util.List;

public class Pin {

    private final int x, y;
    private final List<Note> notes = new ArrayList<>();

    public Pin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isEmpty() {
        return notes.isEmpty();
    }

    public void addIfMissing(Note n) {
        if (notes.contains(n)) return;
        notes.add(n);
        n.addPin();
    }

    public void removeAll() {
        for (Note n : notes) n.removePin();
        notes.clear();
    }

    public void removeNotes(List<Note> gone) {
        for (Note n : gone) {
            if (notes.remove(n)) n.removePin();
        }
    }
}
