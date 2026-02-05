import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket sock;
    private final Board board;

    public ClientHandler(Socket sock, Board board) {
        this.sock = sock;
        this.board = board;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintWriter(sock.getOutputStream(), true);

            out.println("BOARD " + board.getBoardW() + " " + board.getBoardH());
            out.println("NOTE " + board.getNoteW() + " " + board.getNoteH());
            out.println(board.colorsLine());

            String line;
            while ((line = in.readLine()) != null) {
                String resp = Protocol.handle(line, board);
                writeResp(out, resp);

                if (Protocol.shouldClose(resp)) {
                    break;
                }
            }
        } catch (IOException e) {
            // client drop / socket died
        } finally {
            try { if (in != null) in.close(); } catch (IOException e) { }
            if (out != null) out.close();
            try { sock.close(); } catch (IOException e) { }
        }
    }

    private void writeResp(PrintWriter out, String resp) {
        if (resp == null) {
            out.println("ERROR INVALID_FORMAT Invalid request");
            return;
        }

        String[] lines = resp.split("\n", -1);
        for (String s : lines) {
            out.println(s);
        }
    }
}
