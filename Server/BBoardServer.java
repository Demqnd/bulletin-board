import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BBoardServer {

    public static void main(String[] args) {
        if (args.length < 6) {
            usage();
            return;
        }

        int port = mustPosInt(args[0], "port");
        int bw = mustNonNegInt(args[1], "board_width");
        int bh = mustNonNegInt(args[2], "board_height");
        int nw = mustNonNegInt(args[3], "note_width");
        int nh = mustNonNegInt(args[4], "note_height");

        String[] colors = new String[args.length - 5];
        for (int i = 5; i < args.length; i++) {
            colors[i - 5] = args[i];
        }
        if (colors.length == 0) {
            System.err.println("Error: Must provide at least one color.");
            usage();
            return;
        }

        Board board = new Board(bw, bh, nw, nh, colors);

        try (ServerSocket ss = new ServerSocket(port)) {
            while (true) {
                Socket s = ss.accept();
                new Thread(new ClientHandler(s, board)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void usage() {
        System.err.println(
            "Usage: java BBoardServer <port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>"
        );
        System.exit(1);
    }

    private static int mustPosInt(String s, String name) {
        int v = mustNonNegInt(s, name);
        if (v <= 0) {
            System.err.println("Error: " + name + " must be > 0.");
            System.exit(1);
        }
        return v;
    }

    private static int mustNonNegInt(String s, String name) {
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
