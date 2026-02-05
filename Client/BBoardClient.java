import javax.swing.SwingUtilities;

public class BBoardClient {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
