package dk.dtu;

/*
Author(s): Niklas
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.interfaces.UserDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountTests {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserDatabase userDatabase;
    @BeforeEach
    void clean() {
        userDatabase.wipeUserDatabase();
    }

    @Test
    public void testRegisterUser_Successful() throws Exception {

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\", \"passwordHash\":\"password\"}"))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("successful"));
    }

    @Test
    public void testRegisterUser_badRequest() throws Exception {
        // Pre-create user
        String hashPass = encoder.encode("password");
        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\", \"passwordHash\":\"" + hashPass + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("successful"));

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existinguser\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("unsuccessful"));
    }

    //TODO: Login tests
    @Test
    void loginUser_Successful_returns200AndToken() throws Exception {


        // arrange: create the user through the service so existsName(...) is true
        String hashPass = encoder.encode("password");
        userDatabase.createUser("loginuser", hashPass);
        Map<String, String> map = new HashMap<>();
        map.put("username", "loginuser");
        map.put("passwordHash", "password");
        String payload = mapper.writeValueAsString(map);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("successful"))
                .andExpect(jsonPath("$.token").isNotEmpty());

    }
    @Test
    void loginUser_Unsuccessful_badRequest() throws Exception {
        String payload = mapper.writeValueAsString(
                java.util.Collections.singletonMap("username", "ghost")
        );

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("unsuccessful"));
    }

}
