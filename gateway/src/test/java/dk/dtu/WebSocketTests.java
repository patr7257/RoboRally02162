/*
Author(s): Karl, Benjamin
 */

package dk.dtu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dtu.model.Client;
import dk.dtu.model.Host;
import dk.dtu.model.Lobby;
import dk.dtu.model.User;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class WebSocketTests {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    Server server;

    // -- Web Socket message transfer tests --
    @Test
    void clientToGatewayTest() throws Exception {
        User mockUser = mock(User.class);
        when(mockUser.getUserID()).thenReturn("testUser");
        Lobby mockLobby = mock(Lobby.class);

        when(mockLobby.getLobbyID()).thenReturn("1");
        server.getLobbies().put(mockLobby.getLobbyID(), mockLobby);

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

    @Test
    void gatewayToClientTest() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        Client client = new Client(mock(User.class), mockSession);

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

    @Test
    void hostToGatewayTest() throws Exception {
        Lobby mockLobby = mock(Lobby.class);
        when(mockLobby.getLobbyID()).thenReturn("1");
        server.getLobbies().put(mockLobby.getLobbyID(), mockLobby);
        server.getGameToLobby().put("1", mockLobby.getLobbyID());

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/host"));

        ObjectNode json = createJsonHost(true, "1");
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

    @Test
    void gatewayToHostTest() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session123");
        Host host = new Host();
        host.setSession(mockSession);

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

    // -- Web Socket message handling tests --
    @Test
    void clientToHostTest() throws Exception {
        Host mockHost = mock(Host.class);
        Client mockClient = mock(Client.class);
        User mockUser = mock(User.class);
        when(mockUser.getUserID()).thenReturn("testUser");

        Lobby lobby = new Lobby("1", mockClient, mockHost);
        UUID gameID = UUID.randomUUID();
        lobby.setGameID(gameID);
        lobby.getUserToPlayer().put("testUser", "1");

        server.getLobbies().put(lobby.getLobbyID(), lobby);

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

    @Test
    void hostToClientDirectTest() throws Exception {
        Host mockHost = mock(Host.class);
        Client mockClient1 = mock(Client.class);
        Client mockClient2 = mock(Client.class);
        Lobby lobby = new Lobby("1", mockClient1, mockHost);
        lobby.addPlayer(mockClient2);

        UUID gameID = UUID.randomUUID();
        lobby.setGameID(gameID);

        server.getLobbies().put("1", lobby);
        server.getGameToLobby().put(gameID.toString(), "1");

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("hostSession123");
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/host"));

        ObjectNode json = createJsonHost(false, gameID.toString());
        WebSocketMessage<String> message = new TextMessage(json.toString());

        server.getHostHandler().handleMessage(mockSession, message);

        ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockClient1).handleMessage(captor.capture());
        verify(mockClient2, never()).handleMessage(any(ObjectNode.class));

        ObjectNode actualMsg = captor.getValue();
        assertThat(actualMsg.get("type").asText()).isEqualTo("game");
        assertThat(actualMsg.get("payload").asText()).isEqualTo("");
    }

    @Test
    void hostToClientBroadCastTest() throws Exception {
        System.out.println("Lobbies before test: " + server.getLobbies().keySet());
        System.out.println("Games before test: " + server.getGameToLobby().keySet());
        Host mockHost = mock(Host.class);
        Client mockClient1 = mock(Client.class);
        Client mockClient2 = mock(Client.class);
        Lobby lobby = new Lobby("1", mockClient1, mockHost);
        lobby.addPlayer(mockClient2);

        UUID gameID = UUID.randomUUID();
        lobby.setGameID(gameID);

        server.getLobbies().put("1", lobby);
        server.getGameToLobby().put(gameID.toString(), "1");

        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("hostSession123");
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost/host"));

        ObjectNode json = createJsonHost(true, lobby.getGameID().toString());
        WebSocketMessage<String> message = new TextMessage(json.toString());

        server.getHostHandler().handleMessage(mockSession, message);

        ArgumentCaptor<ObjectNode> captor1 = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockClient1).handleMessage(captor1.capture());

        ArgumentCaptor<ObjectNode> captor2 = ArgumentCaptor.forClass(ObjectNode.class);
        verify(mockClient2).handleMessage(captor2.capture());

        ObjectNode actualMsg1 = captor1.getValue();
        assertThat(actualMsg1.get("type").asText()).isEqualTo("game");
        assertThat(actualMsg1.get("payload").asText()).isEqualTo("");

        ObjectNode actualMsg2 = captor2.getValue();
        assertThat(actualMsg2.get("type").asText()).isEqualTo("game");
        assertThat(actualMsg2.get("payload").asText()).isEqualTo("");
    }

    private ObjectNode createJsonHost(boolean broadCast, String gameID) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("type", "stateSnapshot");
        root.put("delivery", broadCast ? "BROADCAST" : "DIRECT");

        ObjectNode meta = JsonUtil.createObjectNode();

        ObjectNode game = JsonUtil.createObjectNode();
        game.put("gameID", gameID);
        meta.set("game", game);

        ObjectNode player = JsonUtil.createObjectNode();
        player.put("playerID", 1);
        meta.set("player", player);

        root.set("meta", meta);

        ObjectNode payload = JsonUtil.createObjectNode();
        root.set("payload", payload);

        return root;
    }

}
