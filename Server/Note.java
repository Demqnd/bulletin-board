public class Note {
    public final int x, y, w, h;
    public final String color;
    public final String message;

    public Note(int x, int y, int w, int h, String color, String message) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.color = color;
        this.message = message;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public boolean completelyOverlaps(Note other) {
        return this.x == other.x && this.y == other.y && this.w == other.w && this.h == other.h;
    }
}
