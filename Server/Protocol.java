public class Protocol {

    public static String handle(String line, Board board) {
        if (line == null) {
            return "ERROR INVALID_FORMAT";
        }

        String trimmed = line.trim();
        if (trimmed.length() == 0) {
            return "ERROR INVALID_FORMAT";
        }

        // Only implement DISCONNECT for Phase 0.
        if (isDisconnect(trimmed)) {
            // Must be exactly: DISCONNECT (no extra tokens)
            if (trimmed.equals("DISCONNECT")) {
                return "OK DISCONNECTED";
            }
            return "ERROR INVALID_FORMAT";
        }

        // Everything else will be implemented in Phase 1/2.
        return "ERROR INVALID_FORMAT";
    }

    public static boolean isDisconnect(String line) {
        if (line == null) return false;
        String t = line.trim();
        return t.startsWith("DISCONNECT");
    }
}
