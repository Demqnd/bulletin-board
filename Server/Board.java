public class Board {

    private final int boardWidth;
    private final int boardHeight;
    private final int noteWidth;
    private final int noteHeight;
    private final String[] colors;

    public Board(int boardWidth, int boardHeight, int noteWidth, int noteHeight, String[] colors) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.noteWidth = noteWidth;
        this.noteHeight = noteHeight;
        this.colors = colors;
    }

    public int getBoardWidth() {
        return boardWidth;
    }

    public int getBoardHeight() {
        return boardHeight;
    }

    public int getNoteWidth() {
        return noteWidth;
    }

    public int getNoteHeight() {
        return noteHeight;
    }

    public String[] getColors() {
        return colors;
    }

    public String getColorsLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("COLORS ").append(colors.length);
        for (int i = 0; i < colors.length; i++) {
            sb.append(" ").append(colors[i]);
        }
        return sb.toString();
    }
}
