package dk.dtu.shared;
//Manages the sessions of logged in users.

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Niklas Emil Lysdal
 * @author Lizette Bloch Dahl Nikolajsen
 */
@Service
public class SessionManager {

    private final Set<String> loggedInUsers = ConcurrentHashMap.newKeySet();
    public SessionManager() {}

    public void logInUser(String loggedInUser) {

        this.loggedInUsers.add(loggedInUser);
    }
    public void logOutUser(String loggedOutUser) {

        this.loggedInUsers.remove(loggedOutUser);
    }


    public boolean isLoggedIn(String loggedInUser) {
        return this.loggedInUsers.contains(loggedInUser);
    }
    public boolean isLoggedOut(String loggedInUser) {
        return !isLoggedIn(loggedInUser);
    }
}
