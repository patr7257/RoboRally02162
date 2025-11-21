package dk.dtu.config;

import dk.dtu.dto.UserToken;
import dk.dtu.model.database.DynamicUserDatabase;

import dk.dtu.model.User;
import dk.dtu.interfaces.UserDatabase;
import dk.dtu.shared.AuthManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * @author Niklas Emil Lysdal
 * @author Bjarke Søderhamn Petersen
 */
@Component
public class ClientHandshakeInterceptor implements HandshakeInterceptor {

    private final UserDatabase userDatabase;
    private final AuthManager authManager;
    /**
     * @author Niklas Emil Lysdal
     */

    @Autowired
    public ClientHandshakeInterceptor(DynamicUserDatabase userDatabase, AuthManager authManager) {
        this.userDatabase = userDatabase;
        this.authManager = authManager;
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
        String query = uri.getQuery();

        if (query != null && query.startsWith("token=")) {
            String token = query.substring("token=".length());


            if (!authManager.validateToken(token)) {
                response.setStatusCode(HttpStatus.FORBIDDEN); // 403
                return false;
            }

            UserToken userToken = authManager.extractUserToken(token);

            User us = userDatabase.findUserById(userToken.userID());
            if (us == null) {
                response.setStatusCode(HttpStatus.FORBIDDEN); // extra safety
                return false;
            }
            attributes.put("user", us);
            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED); // 401
        return false;
    }
    /**
     * @author Niklas Emil Lysdal
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                                ServerHttpResponse response,
                                WebSocketHandler wsHandler,
                                Exception exception) {

    }
}