import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoardPanel extends JPanel {

    // Config from handshake
    private int boardW = 200, boardH = 100;
    private int noteW = 20, noteH = 10;

    // State from GET / GET PINS
    private List<NoteView> notes = new ArrayList<>();
    private List<PinView> pins = new ArrayList<>();

    public BoardPanel() {
        setPreferredSize(new Dimension(900, 450));
        setBorder(BorderFactory.createTitledBorder("Bulletin Board"));
    }

    public void setConfig(int boardW, int boardH, int noteW, int noteH) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;
        repaint();
    }

    public void setState(List<NoteView> notes, List<PinView> pins) {
        this.notes = (notes == null) ? new ArrayList<>() : notes;
        this.pins = (pins == null) ? new ArrayList<>() : pins;
        repaint();
    }

    // Coordinate scaling: protocol coords -> pixels
    private int sx(int x) { return (int)Math.round(x * (getWidth()  / (double)boardW)); }
    private int sy(int y) { return (int)Math.round(y * (getHeight() / (double)boardH)); }
    private int sw(int w) { return (int)Math.round(w * (getWidth()  / (double)boardW)); }
    private int sh(int h) { return (int)Math.round(h * (getHeight() / (double)boardH)); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(225, 215, 170)); // board-ish
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Notes
        for (NoteView n : notes) {
            int px = sx(n.x);
            int py = sy(n.y);
            int pw = sw(noteW);
            int ph = sh(noteH);

            g2.setColor(n.awtColor);
            g2.fillRoundRect(px, py, pw, ph, 14, 14);

            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(px, py, pw, ph, 14, 14);

            // pinned outline
            if (n.pinned) {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(px - 1, py - 1, pw + 2, ph + 2, 14, 14);
                g2.setStroke(new BasicStroke(1));
            }

            // Text
            g2.setColor(Color.BLACK);
            drawWrappedText(g2, n.message, px + 6, py + 16, pw - 12, ph - 10);
        }

        // Pins (on top)
        g2.setColor(Color.BLACK);
        int r = Math.max(4, Math.min(getWidth(), getHeight()) / 90);
        for (PinView p : pins) {
            int px = sx(p.x);
            int py = sy(p.y);
            g2.fillOval(px - r, py - r, 2 * r, 2 * r);
        }
    }

    private void drawWrappedText(Graphics g, String text, int x, int y, int maxW, int maxH) {
        if (text == null) return;
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        int curY = y;

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String next = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(next) <= maxW) {
                line.setLength(0);
                line.append(next);
            } else {
                g.drawString(line.toString(), x, curY);
                curY += lineH;
                if (curY > y + maxH) return;
                line.setLength(0);
                line.append(w);
            }
        }
        if (!line.isEmpty() && curY <= y + maxH) {
            g.drawString(line.toString(), x, curY);
        }
    }

    // --- Simple view models for drawing ---
    public static class NoteView {
        public final int x, y;
        public final String message;
        public final Color awtColor;
        public final boolean pinned;

        public NoteView(int x, int y, String message, Color awtColor, boolean pinned) {
            this.x = x;
            this.y = y;
            this.message = message;
            this.awtColor = awtColor;
            this.pinned = pinned;
        }
    }

    public static class PinView {
        public final int x, y;
        public PinView(int x, int y) { this.x = x; this.y = y; }
    }
}
