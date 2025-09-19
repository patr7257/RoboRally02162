package dk.dtu.dto;
/*
Author(s): Lizette
 */

public class LoginRequest {
    public String username;
    public String passwordHash;

    public LoginRequest() {
        // Default constructor needed for JSON deserialization
    }

    public LoginRequest(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}