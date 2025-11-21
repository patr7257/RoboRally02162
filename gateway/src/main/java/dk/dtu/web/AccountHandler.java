package dk.dtu.web;

import dk.dtu.model.database.DynamicUserDatabase;

import dk.dtu.model.User;
import dk.dtu.interfaces.UserDatabase;
import dk.dtu.dto.AuthResponse;
import dk.dtu.dto.LoginRequest;
import dk.dtu.dto.RegisterRequest;
import dk.dtu.shared.AuthManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Niklas Emil Lysdal
 * @author Kajsa Alice Ulrika Berlstedt
 */
@RestController
@RequestMapping("/api")
public class AccountHandler {

    private final UserDatabase userDatabase;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthManager authManager;
    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Niklas Emil Lysdal
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public AccountHandler(DynamicUserDatabase userDatabase,AuthManager authManager) {
        this.userDatabase = userDatabase;
        this.authManager = authManager;
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Niklas Emil Lysdal
     * @author Kajsa Alice Ulrika Berlstedt
     */
    @PostMapping("/users/create")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        // Expect: { "username": "...", "passwordHash": "sha256hex..." }
        if (req == null || req.username == null || req.username.isBlank()
                || req.passwordHash == null || req.passwordHash.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("unsuccessful", "username and password required", null, null));
        }
        if (userDatabase.existsName(req.username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthResponse("unsuccessful", "Username already exists", null, null));
        }

        // Store bcrypt( sha256(password) )
        String storedBcrypt = encoder.encode(req.passwordHash);
        userDatabase.createUser(req.username, storedBcrypt);

        String token = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("successful", "Registered", token, req.username));
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Niklas Emil Lysdal
     * @author Kajsa Alice Ulrika Berlstedt
     */
    @PostMapping("/users/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        // Expect: { "username": "...", "passwordHash": "sha256hex..." }
        if (req == null || req.username == null || req.username.isBlank()
                || req.passwordHash == null || req.passwordHash.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("unsuccessful", "username and password required", null, null));
        }
        if (!userDatabase.existsNamePassword(req.username, req.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("unsuccessful", "User not found", null, null));
        }

        User user = userDatabase.findUserByNamePassword(req.username,req.passwordHash); // must return stored hash
        if (user == null || user.getPasswordHash() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("unsuccessful", "User not found", null, null));
        }

        boolean ok = encoder.matches(req.passwordHash, user.getPasswordHash());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("unsuccessful", "Invalid credentials", null, null));
        }

        String token = authManager.getUserToken(user.getUserID()); // replace with JWT later
        return ResponseEntity.ok(new AuthResponse("successful", "Logged in", token, user.getUserID()));
    }
}