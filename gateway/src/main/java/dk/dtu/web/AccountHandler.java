package dk.dtu.web;

import dk.dtu.dto.*;
import dk.dtu.model.database.DynamicUserDatabase;

import dk.dtu.model.User;
import dk.dtu.interfaces.UserDatabase;
import dk.dtu.shared.AuthManager;
import dk.dtu.shared.SessionManager;
import dk.dtu.util.APIUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Niklas Emil Lysdal
 * @author Kajsa Alice Ulrika Berlstedt
 * @author Weihao Mo
 * @author Karl Johannes Agerbo
 */
@RestController
@RequestMapping("/api")
public class AccountHandler {

    private final UserDatabase userDatabase;
    private final AuthManager authManager;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SessionManager sessionManager;
    // Track which users are logged in (by userID)

    private final ApplicationEventPublisher eventPublisher;

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Niklas Emil Lysdal
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public AccountHandler(DynamicUserDatabase userDatabase, AuthManager authManager, SessionManager sessionManager,ApplicationEventPublisher eventPublisher) {
        this.userDatabase = userDatabase;
        this.authManager = authManager;
        this.sessionManager = sessionManager;
        this.eventPublisher=eventPublisher;
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

        User user = userDatabase.findUserByNamePassword(req.username, req.passwordHash); // must return stored hash
        if (user == null || user.getPasswordHash() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("unsuccessful", "User not found", null, null));
        }

        boolean ok = encoder.matches(req.passwordHash, user.getPasswordHash());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("unsuccessful", "Invalid credentials", null, null));
        }
        // Prevent login if already logged in
        String userID = user.getUserID();

        if (sessionManager.isLoggedIn(userID)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthResponse("unsuccessful", "User already logged in", null, userID));
        }

        sessionManager.logInUser(userID);

        String token = authManager.getUserToken(user.getUserID()); // replace with JWT later
        return ResponseEntity.ok(new AuthResponse("successful", "Logged in", token, user.getUserID()));
    }

    @PostMapping("/users/logout")
    public ResponseEntity<AuthResponse> logout(@RequestParam String userID) {
        sessionManager.logOutUser(userID);
        return ResponseEntity.ok(new AuthResponse("successful", "Logged out", null, userID));
    }

   @PostMapping("/users/changeUsername")
    public ResponseEntity<String> changeUsername(@RequestParam String newUsername) {
        ChangeUserNameResponse result  =userDatabase.changeUsername(APIUtil.getCallerID(), newUsername);
        return switch (result) {
            case SUCCESS-> {
                eventPublisher.publishEvent(new UserNameUpdateEvent(APIUtil.getCallerID(), newUsername));
                yield ResponseEntity.ok("successful");
            }
            case USERNAME_ALREADY_EXISTS-> ResponseEntity.status(HttpStatus.CONFLICT).body("USERNAME_ALREADY_EXISTS");
            case NO_SUCH_USER-> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NO_SUCH_USER");
            default-> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        };
   }

    /**
     * @author Weihao Mo
     * @author Karl Johannes Agerbo
     */
   @PostMapping("/users/delete")
    public ResponseEntity<AuthResponse> deleteUser() {
        String userID = APIUtil.getCallerID();
        sessionManager.logOutUser(userID);
        boolean deleted = userDatabase.deleteUser(userID);

        if (deleted) {
            return ResponseEntity.ok(new AuthResponse("successful", "user deleted", null, null));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse("unsuccessful", "Server Error", null, null));
        }
   }
}