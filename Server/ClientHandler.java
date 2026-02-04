import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Board board;

    public ClientHandler(Socket socket, Board board) {
        this.socket = socket;
        this.board = board;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            sendHandshake(out);

            String line;
            while ((line = in.readLine()) != null) {
                String response = Protocol.handle(line, board);

                out.println(response);

                if (Protocol.isDisconnect(line)) {
                    break;
                }
            }
        } catch (IOException e) {
            // Client disconnects / network issues should not crash the server.
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void sendHandshake(PrintWriter out) {
        out.println("BOARD " + board.getBoardWidth() + " " + board.getBoardHeight());
        out.println("NOTE " + board.getNoteWidth() + " " + board.getNoteHeight());
        out.println(board.getColorsLine());
    }
}
