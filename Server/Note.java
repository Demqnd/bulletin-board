public class Note {

    private final int x, y;
    private final int w, h;
    private final String color;
    private final String msg;

    private int pinCount = 0;

    public Note(int x, int y, int w, int h, String color, String msg) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.color = color;
        this.msg = msg;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < (x + w) && py >= y && py < (y + h);
    }

    public boolean isPinned() {
        return pinCount > 0;
    }

    public void addPin() {
        pinCount++;
    }

    public void removePin() {
        if (pinCount > 0) pinCount--;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getW() { return w; }
    public int getH() { return h; }
    public String getColor() { return color; }
    public String getMsg() { return msg; }
}
