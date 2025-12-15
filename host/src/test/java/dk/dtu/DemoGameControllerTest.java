package dk.dtu;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.*;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.effects.*;
import dk.dtu.infrastructure.web.GameController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
public class DemoGameControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean GameManager gameManager;
    @MockitoBean BoardAPI boardAPI;

    /**
     * @author William Pii Jæger
     */
    @Test
    void startDemoGame_loadsCoordinateBasedBoard() throws Exception {
        UUID fixed = UUID.randomUUID();
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class), any(Map.class), any(DamageDecks.class)))
                .thenReturn(fixed);
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class)))
                .thenReturn(fixed);

        String body = """
                {
                  "amountPlayers": 2,
                  "board": {
                    "width": 10,
                    "height": 12,
                    "tiles": [
                      {
                        "coord": { "x": 5, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "ANTENNA", "direction": "S" }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 1, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 1 }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 2, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 2 }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 5, "y": 5 },
                        "tile": {
                          "effects": [
                            { "kind": "CHECKPOINT", "number": 1 }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 7, "y": 8 },
                        "tile": {
                          "effects": [
                            { "kind": "geardto", "rotation": "RIGHT" }
                          ]
                        }
                      }
                    ],
                    "decks": {}
                  }
                }
                """;

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);

        mvc.perform(post("/startDemoGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameID").exists())
                .andExpect(jsonPath("$.message").value("Demo game started successfully"));

        verify(gameManager, times(1)).startGame(
                boardCaptor.capture(),
                any(BoardAPI.class),
                any(List.class),
                any(Map.class),
                any(DamageDecks.class)
        );

        Board capturedBoard = boardCaptor.getValue();

        assertEquals(10, capturedBoard.getWidth());
        assertEquals(12, capturedBoard.getHeight());

        Tile antennaTile = capturedBoard.getTile(5, 0);
        assertTrue(antennaTile.getEffects().stream().anyMatch(e -> e instanceof Antenna));
        Antenna antenna = (Antenna) antennaTile.getEffects().stream()
                .filter(e -> e instanceof Antenna)
                .findFirst()
                .orElseThrow();
        assertEquals(Direction.S, antenna.direction());

        Tile startTile1 = capturedBoard.getTile(1, 0);
        assertTrue(startTile1.getEffects().stream().anyMatch(e -> e instanceof StartingTile));

        Tile startTile2 = capturedBoard.getTile(2, 0);
        assertTrue(startTile2.getEffects().stream().anyMatch(e -> e instanceof StartingTile));

        Tile checkpointTile = capturedBoard.getTile(5, 5);
        assertTrue(checkpointTile.getEffects().stream().anyMatch(e -> e instanceof Checkpoint));
        Checkpoint checkpoint = (Checkpoint) checkpointTile.getEffects().stream()
                .filter(e -> e instanceof Checkpoint)
                .findFirst()
                .orElseThrow();
        assertEquals(1, checkpoint.number());

        Tile gearTile = capturedBoard.getTile(7, 8);
        assertTrue(gearTile.getEffects().stream().anyMatch(e -> e instanceof Gear));
        Gear gear = (Gear) gearTile.getEffects().stream()
                .filter(e -> e instanceof Gear)
                .findFirst()
                .orElseThrow();
        assertEquals(Rotation.RIGHT, gear.rotation());

        Tile emptyTile = capturedBoard.getTile(0, 0);
        assertTrue(emptyTile.getEffects().isEmpty());
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void startDemoGame_loadsDecksWithCards() throws Exception {
        UUID fixed = UUID.randomUUID();
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class), any(Map.class), any(DamageDecks.class)))
                .thenReturn(fixed);

        String body = """
                {
                  "amountPlayers": 2,
                  "board": {
                    "width": 5,
                    "height": 5,
                    "tiles": [
                      {
                        "coord": { "x": 1, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 1 }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 2, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 2 }
                          ]
                        }
                      }
                    ],
                    "decks": {
                      "1": {
                        "drawPile": [
                          { "action": "MOVE", "steps": 1 },
                          { "action": "ROTATERIGHT", "steps": 0 }
                        ],
                        "discardPile": [],
                        "hand": [
                          { "action": "MOVE", "steps": 2 },
                          { "action": "UTURN", "steps": 0 }
                        ]
                      },
                      "2": {
                        "drawPile": [
                          { "action": "MOVE", "steps": 1 }
                        ],
                        "discardPile": [
                          { "action": "ROTATELEFT", "steps": 0 }
                        ],
                        "hand": [
                          { "action": "MOVE", "steps": 3 }
                        ]
                      }
                    }
                  }
                }
                """;

        ArgumentCaptor<Map<Integer, Deck>> deckCaptor = ArgumentCaptor.forClass(Map.class);

        mvc.perform(post("/startDemoGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Demo game started successfully"));

        verify(gameManager, times(1)).startGame(
                any(Board.class),
                any(BoardAPI.class),
                any(List.class),
                deckCaptor.capture(),
                any(DamageDecks.class)
        );

        Map<Integer, Deck> capturedDecks = deckCaptor.getValue();

        assertTrue(capturedDecks.containsKey(1));
        Deck deck1 = capturedDecks.get(1);
        assertEquals(2, deck1.getDrawPile().size());
        assertEquals(0, deck1.getDiscardPile().size());
        assertEquals(2, deck1.getHand().size());

        assertTrue(capturedDecks.containsKey(2));
        Deck deck2 = capturedDecks.get(2);
        assertEquals(1, deck2.getDrawPile().size());
        assertEquals(1, deck2.getDiscardPile().size());
        assertEquals(1, deck2.getHand().size());
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void startDemoGame_skipsInvalidCoordinates() throws Exception {
        UUID fixed = UUID.randomUUID();
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class), any(Map.class), any(DamageDecks.class)))
                .thenReturn(fixed);

        String body = """
                {
                  "amountPlayers": 1,
                  "board": {
                    "width": 5,
                    "height": 5,
                    "tiles": [
                      {
                        "coord": { "x": 1, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 1 }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 99, "y": 99 },
                        "tile": {
                          "effects": [
                            { "kind": "CHECKPOINT", "number": 1 }
                          ]
                        }
                      }
                    ],
                    "robots": [],
                    "decks": {}
                  }
                }
                """;

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);

        mvc.perform(post("/startDemoGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(gameManager, times(1)).startGame(
                boardCaptor.capture(),
                any(BoardAPI.class),
                any(List.class),
                any(Map.class),
                any(DamageDecks.class)
        );

        Board capturedBoard = boardCaptor.getValue();

        Tile startTile = capturedBoard.getTile(1, 0);
        assertTrue(startTile.getEffects().stream().anyMatch(e -> e instanceof StartingTile));

        boolean hasCheckpoint = false;
        for (int x = 0; x < capturedBoard.getWidth(); x++) {
            for (int y = 0; y < capturedBoard.getHeight(); y++) {
                Tile tile = capturedBoard.getTile(x, y);
                if (tile.getEffects().stream().anyMatch(e -> e instanceof Checkpoint)) {
                    hasCheckpoint = true;
                    break;
                }
            }
        }
        assertFalse(hasCheckpoint, "Checkpoint at invalid coordinates should be skipped");
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void startDemoGame_positionsRobotsDirectly() throws Exception {
        UUID fixed = UUID.randomUUID();
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class), any(Map.class), any(DamageDecks.class)))
                .thenReturn(fixed);

        String body = """
                {
                  "amountPlayers": 3,
                  "board": {
                    "width": 10,
                    "height": 10,
                    "tiles": [
                      {
                        "coord": { "x": 5, "y": 5 },
                        "tile": {
                          "effects": [
                            { "kind": "CHECKPOINT", "number": 1 }
                          ]
                        }
                      }
                    ],
                    "robots": [
                      {
                        "id": 1,
                        "x": 2,
                        "y": 3,
                        "facing": "N"
                      },
                      {
                        "id": 2,
                        "x": 7,
                        "y": 8,
                        "facing": "E"
                      },
                      {
                        "id": 3,
                        "x": 1,
                        "y": 1,
                        "facing": "W"
                      }
                    ],
                    "decks": {}
                  }
                }
                """;

        ArgumentCaptor<List<Robot>> robotsCaptor = ArgumentCaptor.forClass(List.class);

        mvc.perform(post("/startDemoGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameID").exists())
                .andExpect(jsonPath("$.message").value("Demo game started successfully"));

        verify(gameManager, times(1)).startGame(
                any(Board.class),
                any(BoardAPI.class),
                robotsCaptor.capture(),
                any(Map.class),
                any(DamageDecks.class)
        );

        List<Robot> capturedRobots = robotsCaptor.getValue();

        assertEquals(3, capturedRobots.size());

        Robot robot1 = capturedRobots.stream()
                .filter(r -> r.getId() == 1)
                .findFirst()
                .orElseThrow();
        assertEquals(2, robot1.getX());
        assertEquals(3, robot1.getY());
        assertEquals(Direction.N, robot1.getDirection());

        Robot robot2 = capturedRobots.stream()
                .filter(r -> r.getId() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals(7, robot2.getX());
        assertEquals(8, robot2.getY());
        assertEquals(Direction.E, robot2.getDirection());

        Robot robot3 = capturedRobots.stream()
                .filter(r -> r.getId() == 3)
                .findFirst()
                .orElseThrow();
        assertEquals(1, robot3.getX());
        assertEquals(1, robot3.getY());
        assertEquals(Direction.W, robot3.getDirection());
    }

    /**
     * @author William Pii Jæger
     */
    @Test
    void startDemoGame_fallsBackToStartingTiles() throws Exception {
        UUID fixed = UUID.randomUUID();
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class), any(Map.class), any(DamageDecks.class)))
                .thenReturn(fixed);

        String body = """
                {
                  "amountPlayers": 2,
                  "board": {
                    "width": 5,
                    "height": 5,
                    "tiles": [
                      {
                        "coord": { "x": 1, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 1 }
                          ]
                        }
                      },
                      {
                        "coord": { "x": 2, "y": 0 },
                        "tile": {
                          "effects": [
                            { "kind": "startingtile", "playerId": 2 }
                          ]
                        }
                      }
                    ],
                    "robots": [],
                    "decks": {}
                  }
                }
                """;

        ArgumentCaptor<List<Robot>> robotsCaptor = ArgumentCaptor.forClass(List.class);

        mvc.perform(post("/startDemoGame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(gameManager, times(1)).startGame(
                any(Board.class),
                any(BoardAPI.class),
                robotsCaptor.capture(),
                any(Map.class),
                any(DamageDecks.class)
        );

        List<Robot> capturedRobots = robotsCaptor.getValue();

        assertEquals(2, capturedRobots.size());

        boolean hasRobotAt1_0 = capturedRobots.stream()
                .anyMatch(r -> r.getX() == 1 && r.getY() == 0);
        boolean hasRobotAt2_0 = capturedRobots.stream()
                .anyMatch(r -> r.getX() == 2 && r.getY() == 0);

        assertTrue(hasRobotAt1_0);
        assertTrue(hasRobotAt2_0);
    }
}