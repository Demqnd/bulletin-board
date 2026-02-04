import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class BBoardServer {

    public static void main(String[] args) {
        // <port> <boardW> <boardH> <noteW> <noteH> <color1> ... <colorN>
        if (args.length < 6) {
            System.out.println("Usage:");
            System.out.println("java BBoardServer <port> <boardW> <boardH> <noteW> <noteH> <colors...>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int boardW = Integer.parseInt(args[1]);
        int boardH = Integer.parseInt(args[2]);
        int noteW  = Integer.parseInt(args[3]);
        int noteH  = Integer.parseInt(args[4]);
        String[] colors = Arrays.copyOfRange(args, 5, args.length);

        System.out.println("Starting Bulletin Board Server...");
        System.out.println("Port: " + port);
        System.out.println("Board size: " + boardW + " x " + boardH);
        System.out.println("Note size: " + noteW + " x " + noteH);
        System.out.println("Colors: " + String.join(" ", colors));

        Board board = new Board(boardW, boardH, noteW, noteH, colors);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket, board, boardW, boardH, noteW, noteH, colors).start();
            }
        } catch (IOException e) {
            System.err.println("Server error:");
            e.printStackTrace();
        }
    }
}
