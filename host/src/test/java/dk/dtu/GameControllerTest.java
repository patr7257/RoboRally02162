package dk.dtu;

import dk.dtu.domain.core.GameManager;
import dk.dtu.domain.model.Board;
import dk.dtu.domain.rules.api.BoardAPI;
import dk.dtu.infrastructure.web.GameController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Author(s) William Pii Jæger

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean GameManager gameManager;
    @MockitoBean BoardAPI boardAPI;

    @Test
    void startGame_returnsUuid() throws Exception {
        UUID fixed = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(gameManager.startGame(any(Board.class), any(BoardAPI.class), any(List.class)))
                .thenReturn(fixed);

        String body = """
                {"amountPlayers":4,"boardSize":8}
                """;

        mvc.perform(post("/startGame")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.gameID").value(fixed.toString()));
    }
}