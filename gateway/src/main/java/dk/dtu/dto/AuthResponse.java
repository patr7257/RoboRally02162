package dk.dtu.dto;
/*
Author(s): Lizette
 */

public class AuthResponse {
    public String status;   // "successful" / "error"
    public String message;  // explanation for client
    public String token;    // present if login/register succeeds
    public String username;

    public AuthResponse() {
        // Default constructor for JSON serialization
    }

    public AuthResponse(String status, String message, String token, String username) {
        this.status = status;
        this.message = message;
        this.token = token;
        this.username = username;
    }
}