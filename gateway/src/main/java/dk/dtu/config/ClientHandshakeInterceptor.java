package dk.dtu.config;

import dk.dtu.dto.ClientConnectReason;
import dk.dtu.dto.UserToken;
import dk.dtu.model.database.DynamicUserDatabase;

import dk.dtu.model.User;
import dk.dtu.interfaces.UserDatabase;
import dk.dtu.shared.AuthManager;
import dk.dtu.shared.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Niklas Emil Lysdal
 * @author Bjarke Søderhamn Petersen
 */
@Component
public class ClientHandshakeInterceptor implements HandshakeInterceptor {

    private final UserDatabase userDatabase;
    private final AuthManager authManager;
    private final SessionManager sessionManager;
    /**
     * @author Niklas Emil Lysdal
     */
    @Autowired
    public ClientHandshakeInterceptor(DynamicUserDatabase userDatabase, AuthManager authManager, SessionManager sessionManager) {
        this.userDatabase = userDatabase;
        this.authManager = authManager;
        this.sessionManager = sessionManager;
    }

    /**
     * @author Niklas Emil Lysdal
     * @author Bjarke Søderhamn Petersen
     */

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        URI uri = request.getURI();


        Map<String, List<String>> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String token = queryParams.containsKey("token") ? queryParams.get("token").get(0) : null;
        String rawReason = queryParams.containsKey("reason") ? queryParams.get("reason").get(0) : null;

        if (token == null || !authManager.validateToken(token)) {
            attributes.put("error","invalid token");//pass and kick
            return true;
        }
        if (rawReason == null ) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return true;
        }


        UserToken userToken = authManager.extractUserToken(token);

        User us = userDatabase.findUserById(userToken.userID());

        if (us == null) {
            attributes.put("error","unknown user");//pass and kick
            return true;
        }
        if (!sessionManager.isLoggedIn(us.getUserID())) {
            attributes.put("error","not logged in"); //pass and kick
            return true;
        }

        attributes.put("user", us);
        attributes.put("token", token);
        try {
            ClientConnectReason reason = ClientConnectReason.valueOf(rawReason);
            attributes.put("reason", reason);
        } catch ( IllegalArgumentException e ) {
            attributes.put("reason", ClientConnectReason.LOGIN);
        }

        return true;
    }


    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
