package dk.dtu.config;

/*
Author(s): Niklas, Bjarke
 */

import dk.dtu.model.User;
import dk.dtu.interfaces.UserDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
public class ClientHandshakeInterceptor implements HandshakeInterceptor {

    private final UserDatabase userDatabase;

    @Autowired
    public ClientHandshakeInterceptor(UserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) throws Exception {

        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null && query.startsWith("token=")) {
            String token = query.substring("token=".length());

            //example default user
           // userDatabase.createUser("User", "password");

            if (!userDatabase.existsID(token)) {
                response.setStatusCode(HttpStatus.FORBIDDEN); // 403
                return false;
            }

            User us = userDatabase.findUserById(token);
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

    @Override
    public void afterHandshake(ServerHttpRequest request,
                                ServerHttpResponse response,
                                WebSocketHandler wsHandler,
                                Exception exception) {

    }
}