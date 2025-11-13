package dk.dtu.dto;
/**
 * @author Lizette Bloch Dahl Nikolajsen
 */

public class RegisterRequest {
    public String username;
    public String passwordHash;

    public RegisterRequest() {
        // Default constructor needed for JSON deserialization
    }

    public RegisterRequest(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}