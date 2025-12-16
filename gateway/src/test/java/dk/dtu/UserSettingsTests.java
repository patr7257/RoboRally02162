package dk.dtu;

import dk.dtu.dto.AuthResponse;
import dk.dtu.dto.ClientUpdateReason;
import dk.dtu.model.Client;
import dk.dtu.model.Lobby;
import dk.dtu.model.User;
import dk.dtu.model.database.DynamicUserDatabase;
import dk.dtu.observer.ClientObserver;
import dk.dtu.shared.ServerManager;
import dk.dtu.web.AccountHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author  Niklas Emil Lysdal
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserSettingsTests {

    @Mock
    ClientObserver mockClientObserver;
    @Mock
    WebSocketSession session;
    @Autowired
    ServerManager serverManager;
    @Autowired
    DynamicUserDatabase userDatabase;
    @Autowired
    AccountHandler accountHandler;

    @Mock
    ScheduledExecutorService mockScheduler;
    @Mock
    ScheduledFuture mockFuture;

    @BeforeEach
    void setUp() {
        userDatabase.wipeUserDatabase();
        Client.setScheduler(mockScheduler);
        lenient().when(mockScheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mockFuture);
    }

    @AfterEach
    void tearDown() {
        userDatabase.wipeUserDatabase();
        Client.setScheduler(Executors.newScheduledThreadPool(1));
    }

    /**
     * @author  Niklas Emil Lysdal
     */
    @Test
    public void changeUsernameSuccess() throws IOException {

        when(session.getId()).thenReturn("sess-1");
        User u = userDatabase.createUser("originalName","testPassword");

        Client c = new Client(u,session);
        c.addObserver(mockClientObserver);
        Map<String, Object> sessionAttributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(sessionAttributes);
        serverManager.putClient(c);
        Lobby lob = serverManager.createLobby(c,"testLobby",4);
        User u2 = new User("1","testExtra","testPassword2");
        WebSocketSession mockSession2 = mock(WebSocketSession.class);
        when(mockSession2.isOpen()).thenReturn(true);
        when(mockSession2.getId()).thenReturn("sess-2");

        Client c2 = new Client (u2,mockSession2);
        serverManager.putClient(c2);
        lob.addPlayer(c2);

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(u.getUserID());

        SecurityContextHolder.setContext(securityContext);
        reset(mockSession2);
        when(mockSession2.isOpen()).thenReturn(true);
            ResponseEntity<String> response = accountHandler.changeUsername("newUsername");

        assertThat(c.getUsername()).isEqualTo("newUsername");
        assertThat(u.getName()).isEqualTo("newUsername");
        verify(mockSession2, timeout(2000).atLeastOnce()).sendMessage(any());
        verify(mockClientObserver).handleClientNameUpdate(c);
        assertThat(((User)sessionAttributes.get("user")).getName()).isEqualTo("newUsername");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("successful");
        SecurityContextHolder.clearContext();


    }
    /**
     * @author  Niklas Emil Lysdal
     */
    @Test
    public void changeUsernameNoSuchUser() throws IOException {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn("fakeUser");
        SecurityContextHolder.setContext(securityContext);
        ResponseEntity<String> response = accountHandler.changeUsername("newUsername");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("NO_SUCH_USER");
        SecurityContextHolder.clearContext();
    }

    /**
     * @author  Niklas Emil Lysdal
     */
    @Test
    public void changeUsernameAlreadyExists() throws IOException {
        User u = userDatabase.createUser("name","testPassword");
        userDatabase.createUser("newUsername","testPassword");
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(u.getUserID());
        SecurityContextHolder.setContext(securityContext);
        ResponseEntity<String> response = accountHandler.changeUsername("newUsername");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("USERNAME_ALREADY_EXISTS");

        SecurityContextHolder.clearContext();


    }
    //test username already exists
    //general error doesn't need to be tested.

    /**
     * @author Weihao Mo
     * @author Karl Johannes Agerbo
     */
    @Test
    public void deleteUserSuccess() throws Exception {
        User u = userDatabase.createUser("name","testPassword");
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(u.getUserID());
        SecurityContextHolder.setContext(securityContext);

        ResponseEntity<AuthResponse> response = accountHandler.deleteUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status).isEqualTo("successful");
        assertThat(response.getBody().message).isEqualTo("user deleted");
        assertThat(response.getBody().token).isEqualTo(null);
        assertThat(response.getBody().userID).isEqualTo(null);

        assertThat(userDatabase.findUserById(u.getUserID())).isNull();

        SecurityContextHolder.clearContext();
    }

    /**
     * @author Weihao Mo
     * @author Karl Johannes Agerbo
     */
    @Test
    public void deleteUserFailure() throws Exception {
        User u = userDatabase.createUser("name","testPassword");
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn("nonExistingUser");
        SecurityContextHolder.setContext(securityContext);

        ResponseEntity<AuthResponse> response = accountHandler.deleteUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status).isEqualTo("unsuccessful");
        assertThat(response.getBody().message).isEqualTo("Server Error");
        assertThat(response.getBody().token).isEqualTo(null);
        assertThat(response.getBody().userID).isEqualTo(null);

        assertThat(userDatabase.findUserById(u.getUserID())).isNotNull();

        SecurityContextHolder.clearContext();
    }
}
