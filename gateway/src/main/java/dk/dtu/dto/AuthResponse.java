package dk.dtu.dto;
/*
Author(s): Lizette, Kajsa
 */

public class AuthResponse {
    public String status;   // "successful" / "error"
    public String message;  // explanation for client
    public String token;    // present if login/register succeeds
    public String userID;

    public AuthResponse() {
        // Default constructor for JSON serialization
    }

    public AuthResponse(String status, String message, String token, String userID) {
        this.status = status;
        this.message = message;
        this.token = token;
        this.userID = userID;
    }
}