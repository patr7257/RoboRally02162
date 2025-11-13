package dk.dtu.dto;
/**
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Kajsa Alice Ulrika Berlstedt
 */

public class LoginRequest {
    public String username;
    public String passwordHash;

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    public LoginRequest() {
        // Default constructor needed for JSON deserialization
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */

    public LoginRequest(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}