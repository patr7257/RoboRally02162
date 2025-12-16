package dk.dtu;

import dk.dtu.config.ClientHandshakeInterceptor;
import dk.dtu.dto.*;
import dk.dtu.model.*;
import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.observer.ClientObserver;
import dk.dtu.observer.LobbyObserver;
import dk.dtu.shared.AuthManager;
import dk.dtu.shared.ServerManager;
import dk.dtu.shared.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;


import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Niklas Emil Lysdal
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DisconnectTests {

    @Mock
    WebSocketSession session;
    @Mock
    User user;
    @Mock
    ClientObserver observer;

    @Mock
    ScheduledExecutorService mockScheduler;
    @Mock
    ScheduledFuture mockFuture;

    @Mock
    DynamicUserDatabase database;
    @Mock
    AuthManager authManager;
    @Autowired
    SessionManager sessionManager;

    @Autowired
    Server server;
    @Autowired
    ServerManager serverManager;

    @Autowired
    private  ClientHandshakeInterceptor interceptor;
    @BeforeEach
    void setUp() {

        Client.setScheduler(mockScheduler);


        lenient().when(mockScheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mockFuture);
    }

    @AfterEach
    void tearDown() {

        Client.setScheduler(Executors.newScheduledThreadPool(1));
    }
    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testDisconnectLogout() throws Exception {
        when(session.getId()).thenReturn("sess-1");
        Client client = new Client(user, session);

        Host mockHost = mock(Host.class);
        String userID= "user-1";
        when(user.getUserID()).thenReturn( userID);
        //when(user.getName()).thenReturn("testName");
        LobbyObserver lobbyObserver = mock(LobbyObserver.class);
        Lobby lobby = new Lobby("testLobby","1",client,mockHost,3, false);
        lobby.addObserver(lobbyObserver);

        sessionManager.logInUser(userID);
        assertThat(sessionManager.isLoggedIn(userID)).isTrue();
        serverManager.putClient(client);
        client.addObserver(observer);
        client.handleDisconnect(ClientDisconnectReason.CONNECTION_LOSS);
        ArgumentCaptor<Runnable> cleanupTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockScheduler).schedule(
                cleanupTaskCaptor.capture(), // Catch the task
                eq(10L),                     // Check the time
                eq(TimeUnit.SECONDS)         // Check the unit
        );
        cleanupTaskCaptor.getValue().run();
        verify(observer).handleClientUpdate(ClientUpdateReason.DISCONNECTED, client);
        assertThat(sessionManager.isLoggedOut(userID)).isTrue();
        assertThat(serverManager.getClient(userID)==null).isTrue();
        assertThat(!lobby.hasParticipant(userID)).isTrue();
        verify(lobbyObserver).handleUpdate(LobbyUpdateReason.DESTROYED,lobby);

    }
    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testReconnectSuccess() throws Exception {
        String userID = "user-1";
        when(session.getId()).thenReturn("sess-1");
        when(user.getUserID()).thenReturn(userID);
        //when(user.getName()).thenReturn("testName");
        Host mockHost = mock(Host.class);
        LobbyObserver lobbyObserver = mock(LobbyObserver.class);

        ScheduledFuture mockFuture = mock(ScheduledFuture.class); //allows us to check that it is cancelled.

        when(mockScheduler.schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(mockFuture);


        Client client = new Client(user, session);
        Lobby lobby = new Lobby("testLobby", "1", client, mockHost, 3, false);
        lobby.addObserver(lobbyObserver);
        client.addObserver(observer);
        sessionManager.logInUser(userID);
        serverManager.putClient(client);

        assertThat(sessionManager.isLoggedIn(userID)).isTrue();
        client.handleDisconnect(ClientDisconnectReason.CONNECTION_LOSS);


        verify(mockScheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS)); //verify task is scheduled



        WebSocketSession newSession = mock(WebSocketSession.class);
        //when(newSession.getId()).thenReturn("sess-2");

        client.handleConnect(newSession, ClientConnectReason.RECONNECT);



        verify(mockFuture).cancel(false); //verify task was aborted.


        verify(observer).handleClientUpdate(ClientUpdateReason.RECONNECTED, client);
        verify(observer, never()).handleClientUpdate(ClientUpdateReason.DISCONNECTED, client);
        assertThat(client.getSession()).isEqualTo(newSession);


        assertThat(sessionManager.isLoggedIn(userID)).isTrue();
        assertThat(serverManager.getClient(userID)).isNotNull();
        assertThat(lobby.hasParticipant(userID)).isTrue();
        verify(lobbyObserver, never()).handleUpdate(eq(LobbyUpdateReason.DESTROYED), any());
    }
    /**
     * @author Niklas Emil Lysdal
     */
    @Test
    public void testReconnectTimeout() throws Exception {
        String token = "token-1";
        when(authManager.validateToken(token)).thenReturn(true);
        String userID = "user-1";
        User mockUserData = mock(User.class);
        when(mockUserData.getUserID()).thenReturn(userID);

        when(database.findUserById(userID)).thenReturn(mockUserData);

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);


        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(attributes);

        attributes.put("error", "not logged in");
        when(request.getURI()).thenReturn(new URI("ws://localhost/game?token=" + token+"&reason=LOGIN"));
        Date currentDate = new Date();
        Date futureDate = new Date(System.currentTimeMillis() + 3600 * 1000);
        UserToken mockTokenData = new UserToken(userID, currentDate,futureDate);
        when(authManager.extractUserToken(token)).thenReturn(mockTokenData);
        interceptor =  new ClientHandshakeInterceptor(database,authManager,sessionManager);
        interceptor.beforeHandshake(request,response,wsHandler,attributes);

        assertThat(attributes.get("error").equals("not logged in")).isTrue();


        server.getClientHandler().afterConnectionEstablished(session);
        ArgumentCaptor<CloseStatus> statusCaptor = ArgumentCaptor.forClass(CloseStatus.class);

        verify(session).close(statusCaptor.capture());
        CloseStatus capturedStatus = statusCaptor.getValue();
        assertThat(capturedStatus.getCode()).isEqualTo(4001);
        assertThat(capturedStatus.getReason()).isEqualTo("NOT_LOGGED_IN");


    }

}
