package dk.dtu;

/*
Author(s): Niklas
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserDatabase userDatabase;
    @BeforeEach
    void clean() {
        // use whatever API your Database exposes to remove test users
        userDatabase.deleteUser("testuser");
        userDatabase.deleteUser("existinguser");
    }

    @Test
    public void testRegisterUser_Successful() throws Exception {
        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("successful"));
    }

    @Test
    public void testRegisterUser_Conflict() throws Exception {
        // Pre-create user
        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existinguser\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("successful"));

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existinguser\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("unsuccessful"));
    }

    //TODO: Login tests
    @Test
    void loginUser_Successful_returns200AndToken() throws Exception {
        // arrange: create the user through the service so existsName(...) is true
        userDatabase.createUser("loginuser");

        String payload = mapper.writeValueAsString(
                java.util.Collections.singletonMap("username", "loginuser")
        );

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("successful"))
                .andExpect(jsonPath("$.token").value("loginuser"));

    }
    @Test
    void loginUser_Unsuccessful_returns409() throws Exception {
        String payload = mapper.writeValueAsString(
                java.util.Collections.singletonMap("username", "ghost")
        );

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("unsuccessful"));
    }

}
