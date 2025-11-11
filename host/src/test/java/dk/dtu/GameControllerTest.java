package dk.dtu;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Tile;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.domain.rules.effects.Checkpoint;
import dk.dtu.domain.rules.effects.TileEffect;
import dk.dtu.infrastructure.web.GameController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean GameManager gameManager;
    @MockitoBean BoardAPI boardAPI;

    /**
     * @author William Pii Jæger
     */
    @Test
    void startGame_returnsUuid() throws Exception {
        UUID fixed = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class)))
                .thenReturn(fixed);

        String body = """
                {"amountPlayers":4,"boardSize":10}
                """;

        mvc.perform(post("/startGame")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.gameID").value(fixed.toString()));
    }

    /**
     * @author Weihao Mo
     */
    @Test
    void startGame_noDuplicate() throws Exception {
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class)))
                .thenReturn(UUID.randomUUID());

        String body = """
                {"amountPlayers":4,"boardSize":10}
                """;

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);

        for (int iteration = 0; iteration < 50; iteration++) {
            mvc.perform(post("/startGame")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        verify(gameManager, times(50)).startGame(boardCaptor.capture(), any(BoardAPI.class), any(List.class));

        List<Board> capturedBoards = boardCaptor.getAllValues();
        assertEquals(50, capturedBoards.size(), "Should have captured 50 boards");

        for (int i = 0; i < capturedBoards.size(); i++) {
            Board board = capturedBoards.get(i);

            Map<Tile, List<Integer>> checkpointsByTile = new HashMap<>();

            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    Tile tile = board.getTile(x, y);
                    for (TileEffect effect : tile.getEffects()) {
                        if (effect instanceof Checkpoint checkpoint) {
                            checkpointsByTile.computeIfAbsent(tile, k -> new ArrayList<>())
                                    .add(checkpoint.number());
                        }
                    }
                }
            }

            assertEquals(3, checkpointsByTile.size());

            for (Map.Entry<Tile, List<Integer>> entry : checkpointsByTile.entrySet()) {
                assertEquals(1, entry.getValue().size());
            }

            Set<Integer> checkpointNumbers = new HashSet<>();
            for (List<Integer> numbers : checkpointsByTile.values()) {
                checkpointNumbers.addAll(numbers);
            }
            assertEquals(3, checkpointNumbers.size());
            assertTrue(checkpointNumbers.contains(1));
            assertTrue(checkpointNumbers.contains(2));
            assertTrue(checkpointNumbers.contains(3));
        }
    }
}