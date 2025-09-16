package dk.dtu;

/*
Author(s): Niklas
 */

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccountHandler {

    private UserDatabase userDatabase;


    public AccountHandler(UserDatabase userDatabase) {this.userDatabase = userDatabase;};

    @PostMapping("/users/create")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody JsonNode json) { //TODO: make include password most likely.
        Map<String, String> response = new HashMap<>();
        String username = json.get("username").asText();
        if (userDatabase.existsName(username)){
            response.put("status", "unsuccessful");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        userDatabase.createUser(username);
        response.put("status", "successful");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/users/login") //TODO: change to use passwords as well
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody JsonNode json) {
        Map<String, String> response = new HashMap<>();
        String username = json.get("username").asText();
        if (userDatabase.existsName(username)){
            response.put("status", "successful");
            response.put("token", username);//TODO: change to be actual token
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } else {
            response.put("status", "unsuccessful");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

    }
}
