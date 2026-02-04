import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BBoardServer {

    public static void main(String[] args) {
        if (args.length < 6) {
            printUsageAndExit();
        }

        int port = parsePositiveInt(args[0], "port");
        int boardW = parseNonNegativeInt(args[1], "board_width");
        int boardH = parseNonNegativeInt(args[2], "board_height");
        int noteW = parseNonNegativeInt(args[3], "note_width");
        int noteH = parseNonNegativeInt(args[4], "note_height");

        String[] colors = new String[args.length - 5];
        for (int i = 5; i < args.length; i++) {
            colors[i - 5] = args[i];
        }
        if (colors.length == 0) {
            System.err.println("Error: Must provide at least one color.");
            printUsageAndExit();
        }

        Board board = new Board(boardW, boardH, noteW, noteH, colors);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(clientSocket, board));
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: java BBoardServer <port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>");
        System.exit(1);
    }

    private static int parsePositiveInt(String s, String name) {
        int v = parseNonNegativeInt(s, name);
        if (v <= 0) {
            System.err.println("Error: " + name + " must be > 0.");
            System.exit(1);
        }
        return v;
    }

    private static int parseNonNegativeInt(String s, String name) {
        try {
            int v = Integer.parseInt(s);
            if (v < 0) {
                System.err.println("Error: " + name + " must be >= 0.");
                System.exit(1);
            }
            return v;
        } catch (NumberFormatException e) {
            System.err.println("Error: " + name + " must be an integer.");
            System.exit(1);
            return -1;
        }
    }
}
