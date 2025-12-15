package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.model.*;
import dk.dtu.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.timeout;

/**
 * @author Karl Johannes Agerbo
 * @author Benjamin Benyo Endahl Hansen
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class WebSocketTests {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    Server server;

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void clientToGatewayTest() throws Exception {
        User mockUser = mock(User.class);
        when(mockUser.getUserID()).thenReturn("testUser");
        Lobby mockLobby = mock(Lobby.class);

        when(mockLobby.getLobbyID()).thenReturn("1");
        server.getLobbiesForTest().put(mockLobby.getLobbyID(), mockLobby);

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", mockUser);
        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/client?token=testUser"));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("action", "doSomething");

        ObjectNode fullMessage = mapper.createObjectNode();
        fullMessage.put("lobbyID", "1");
        fullMessage.set("payload", payload);

        WebSocketMessage<String> message = new TextMessage(fullMessage.toString());

        server.getClientHandler().handleMessage(mockSession, message);

        ArgumentCaptor<ObjectNode> jsonCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockLobby).handleClientMessage(any(), jsonCaptor.capture());

        ObjectNode actualMsg = jsonCaptor.getValue();

        assertThat(actualMsg.get("lobbyID").asText()).isEqualTo("1");
        assertThat(actualMsg.get("payload").get("action").asText()).isEqualTo("doSomething");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void gatewayToClientTest() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        when(mockSession.isOpen()).thenReturn(true);
        MessageQueue testQueue = new MessageQueue(mockSession, Runnable::run);
        Client client = new Client(mock(User.class), mockSession, testQueue);


        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "game");
        root.put("payload", "action");
        client.handleMessage(root);

        ArgumentCaptor<WebSocketMessage<?>> wsMessageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(mockSession).sendMessage(wsMessageCaptor.capture());

        String actualMsg = wsMessageCaptor.getValue().getPayload().toString();
        JsonNode json = JsonUtil.parser(actualMsg);

        assertThat(json.get("type").asText()).isEqualTo("game");
        assertThat(json.get("payload").asText()).isEqualTo("action");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void hostToGatewayTest() throws Exception {
        Lobby mockLobby = mock(Lobby.class);
        when(mockLobby.getLobbyID()).thenReturn("1");
        server.getLobbiesForTest().put(mockLobby.getLobbyID(), mockLobby);
        server.getGameToLobbyForTest().put("1", mockLobby.getLobbyID());

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/host"));

        ObjectNode json = createJsonHost(true, "1", "1");
        WebSocketMessage<String> message = new TextMessage(json.toString());

        server.getHostHandler().handleMessage(mockSession, message);

        ArgumentCaptor<ObjectNode> jsonCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockLobby).handleHostMessage(jsonCaptor.capture());

        ObjectNode actualMsg = jsonCaptor.getValue();

        assertThat(actualMsg.get("type").asText()).isEqualTo("stateSnapshot");
        assertThat(actualMsg.get("delivery").asText()).isEqualTo("BROADCAST");
        assertThat(actualMsg.get("meta").get("game").get("gameID").asText()).isEqualTo("1");
        assertThat(actualMsg.get("meta").get("player").get("playerID").asText()).isEqualTo("1");
        assertThat(actualMsg.get("payload").asText()).isEqualTo("");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void gatewayToHostTest() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        when(mockSession.isOpen()).thenReturn(true);
        Host host = new Host();
        host.testSetSession(mockSession, Runnable::run);

        ObjectNode root = JsonUtil.createObjectNode();
        root.put("gameID", "1");
        root.put("type", "submitProgram");
        root.put("playerID", 1);
        root.put("payload", "action");
        host.handleMessage(root);

        ArgumentCaptor<WebSocketMessage<?>> wsMessageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(mockSession).sendMessage(wsMessageCaptor.capture());

        String actualMsg = wsMessageCaptor.getValue().getPayload().toString();
        JsonNode json = JsonUtil.parser(actualMsg);

        assertThat(json.get("gameID").asText()).isEqualTo("1");
        assertThat(json.get("type").asText()).isEqualTo("submitProgram");
        assertThat(json.get("playerID").asInt()).isEqualTo(1);
        assertThat(json.get("payload").asText()).isEqualTo("action");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void clientToHostTest() throws Exception {
        Host mockHost = mock(Host.class);
        Client mockClient = mock(Client.class);
        when(mockClient.getUserID()).thenReturn("client111");
        when(mockClient.getUsername()).thenReturn("client111");
        User mockUser = mock(User.class);
        when(mockUser.getUserID()).thenReturn("testUser");

        Lobby lobby = new Lobby("testName", "1", mockClient, mockHost,6, false);
        UUID gameID = UUID.randomUUID();
        lobby.setGameID(gameID);
        lobby.getUserToPlayer().put("testUser", "1");

        server.getLobbiesForTest().put(lobby.getLobbyID(), lobby);
        lobby.setIsRunning(true);
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", mockUser);
        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/client?token=testUser"));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("action", "doSomething");

        ObjectNode fullMessage = mapper.createObjectNode();
        fullMessage.put("lobbyID", "1");
        fullMessage.set("payload", payload);

        WebSocketMessage<String> message = new TextMessage(fullMessage.toString());

        server.getClientHandler().handleMessage(mockSession, message);

        ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockHost).handleMessage(captor.capture());

        ObjectNode actualMsg = captor.getValue();
        assertThat(actualMsg.get("gameID").asText()).isEqualTo(lobby.getGameID().toString());
        assertThat(actualMsg.get("playerID").asInt()).isEqualTo(1);
        assertThat(actualMsg.get("payload").get("action").asText()).isEqualTo("doSomething");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void hostToClientDirectTest() throws Exception {
        System.out.println("Lobbies before test: " + server.getLobbiesForTest().keySet());
        System.out.println("Games before test: " + server.getGameToLobbyForTest().keySet());
        Host mockHost = mock(Host.class);
        when(mockHost.startGame(anyInt(), anyInt())).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        Client mockClient1 = mock(Client.class);
        when(mockClient1.getUsername()).thenReturn("client111");
        when(mockClient1.getUserID()).thenReturn("testUser1");

        Client mockClient2 = mock(Client.class);
        when(mockClient2.getUserID()).thenReturn("testUser2");
        when(mockClient2.getUsername()).thenReturn("client222");
        Lobby lobby = new Lobby("testName","1", mockClient1, mockHost,6, false);

        lobby.addPlayer(mockClient2);

        lobby.playerMarkedAsReady("testUser1");
        lobby.playerMarkedAsReady("testUser2");

        lobby.startGame(null);
        UUID gameID = lobby.getGameID();

        server.getLobbiesForTest().put("1", lobby);
        server.getGameToLobbyForTest().put(gameID.toString(), "1");

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("hostSession123");
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/host"));
        Map<String, String> userToPlayer = lobby.getUserToPlayer();
        String u1PlayerID = userToPlayer.get("testUser1");
        ObjectNode json = createJsonHost(false, lobby.getGameID().toString(), u1PlayerID);

        WebSocketMessage<String> message = new TextMessage(json.toString());

        Thread.sleep(50);
        server.getHostHandler().handleMessage(mockSession, message);
        Thread.sleep(50);

        ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockClient1, times(6)).handleMessage(captor.capture());
        verify(mockClient2, times(4)).handleMessage(any(ObjectNode.class));

        ObjectNode actualMsg = captor.getValue();
        assertThat(actualMsg.get("type").asText()).isEqualTo("stateSnapshot");
        assertThat(actualMsg.get("payload").asText()).isEqualTo("");
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    @Test
    void hostToClientBroadCastTest() throws Exception {
        System.out.println("Lobbies before test: " + server.getLobbiesForTest().keySet());
        System.out.println("Games before test: " + server.getGameToLobbyForTest().keySet());
        Host mockHost = mock(Host.class);
        when(mockHost.startGame(anyInt(), anyInt())).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        Client mockClient1 = mock(Client.class);
        when(mockClient1.getUsername()).thenReturn("client111");
        when(mockClient1.getUserID()).thenReturn("testUser1");

        Client mockClient2 = mock(Client.class);
        when(mockClient2.getUserID()).thenReturn("testUser2");
        when(mockClient2.getUsername()).thenReturn("client222");
        Lobby lobby = new Lobby("testName", "1", mockClient1, mockHost,6, false);

        lobby.addPlayer(mockClient2);

        lobby.playerMarkedAsReady("testUser1");
        lobby.playerMarkedAsReady("testUser2");

        lobby.startGame(null);
        UUID gameID = lobby.getGameID();

        server.getLobbiesForTest().put("1", lobby);
        server.getGameToLobbyForTest().put(gameID.toString(), "1");

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("hostSession123");
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/host"));

        ObjectNode json = createJsonHost(true, lobby.getGameID().toString(), "1");
        WebSocketMessage<String> message = new TextMessage(json.toString());

        server.getHostHandler().handleMessage(mockSession, message);

        ArgumentCaptor<ObjectNode> captor1 = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockClient1, timeout(1000).times(6)).handleMessage(captor1.capture());

        ArgumentCaptor<ObjectNode> captor2 = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockClient2, timeout(1000).times(5)).handleMessage(captor2.capture());

        List<ObjectNode> allMessages1 = captor1.getAllValues();
        boolean foundSnapshot1 = false;
        for (ObjectNode msg : allMessages1) {
            if ("stateSnapshot".equals(msg.get("type").asText()) && "".equals(msg.get("payload").asText())) {
                foundSnapshot1 = true;
                break;
            }
        }
        assertThat(foundSnapshot1);

        List<ObjectNode> allMessages2 = captor2.getAllValues();
        boolean foundSnapshot2 = false;
        for (ObjectNode msg : allMessages1) {
            if ("stateSnapshot".equals(msg.get("type").asText()) && "".equals(msg.get("payload").asText())) {
                foundSnapshot2 = true;
                break;
            }
        }
        assertThat(foundSnapshot2);

    }

    /**
     * @author Karl Johannes Agerbo
     * @author Benjamin Benyo Endahl Hansen
     */
    private ObjectNode createJsonHost(boolean broadCast, String gameID, String playerID) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "stateSnapshot");
        root.put("delivery", broadCast ? "BROADCAST" : "DIRECT");

        ObjectNode meta = JsonUtil.createObjectNode();

        ObjectNode game = JsonUtil.createObjectNode();
        game.put("gameID", gameID);
        meta.set("game", game);

        ObjectNode player = JsonUtil.createObjectNode();
        player.put("playerID", playerID);
        meta.set("player", player);

        root.set("meta", meta);

        ObjectNode payload = JsonUtil.createObjectNode();
        root.set("payload", payload);

        return root;
    }

}
