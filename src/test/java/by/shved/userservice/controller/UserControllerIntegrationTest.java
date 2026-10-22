package by.shved.userservice.controller;

import by.shved.userservice.repository.UserRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    @Container
    @ServiceConnection
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", postgres::getJdbcUrl);
//        registry.add("spring.datasource.username", postgres::getUsername);
//        registry.add("spring.datasource.password", postgres::getPassword);
//    }

    @Test
    void create_whenRequestIsValid_returnsCreatedUser() throws Exception {
        String email = "ivan" + UUID.randomUUID() + "@example.com";
        String request = """
                {
                    "name": "Ivan",
                    "surname": "Ivanov",
                    "birthDate": "1999-09-29",
                    "email": "%s"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Ivanov"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.active").value(true));
        assertTrue(userRepository.existsByEmail(email));
    }

    @Test
    void create_whenNameIsBlank_returnsBadRequest() throws Exception {
        String request = """
                {
                    "name": "",
                    "surname": "Ivanov",
                    "birthDate": "1999-09-29",
                    "email": "ivan@example.com"
                }
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_whenUserExists_returnsUser() throws Exception {
        long id = createUser();

        mockMvc.perform(get("/api/users/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Ivanov"));
    }

    @Test
    void getById_whenUserDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_whenNameIsProvided_filtersUsersByName() throws Exception {
        createUser("Mikhail", "Abramovich");
        createUser("Stanislav", "Kovalev");

        mockMvc.perform(get("/api/users")
                        .param("name", "Mikhail")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Mikhail"))
                .andExpect(jsonPath("$.content[0].surname").value("Abramovich"));
    }

    @Test
    void getAll_whenSurnameIsProvided_filtersUsersBySurname() throws Exception {
        long ivanId = createUser("Vladislav", "Chernov");
        long petrId = createUser("Artem", "Zhuravlev");

        mockMvc.perform(get("/api/users")
                        .param("surname", "Zhuravlev")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Artem"))
                .andExpect(jsonPath("$.content[0].surname").value("Zhuravlev"));
    }

    @Test
    void getAll_whenNameAndSurnameAreProvided_filtersUsersByBothFields() throws Exception {
        long matchingUserId = createUser("Igor", "Vernik");
        long sameNameUserId = createUser("Igor", "Sidorov");
        long sameSurnameUserId = createUser("Lesha", "Vernik");

        mockMvc.perform(get("/api/users")
                        .param("name", "Igor")
                        .param("surname", "Vernik")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Igor"))
                .andExpect(jsonPath("$.content[0].surname").value("Vernik"));
    }

    @Test
    void update_whenUserExists_updatesUser() throws Exception {
        long id = createUser();
        String email = "updated" + UUID.randomUUID() + "@example.com";
        String request = """
                {
                    "name": "Petr",
                    "surname": "Petrov",
                    "birthDate": "2000-05-15",
                    "email": "%s"
                }
                """.formatted(email);

        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Petr"))
                .andExpect(jsonPath("$.surname").value("Petrov"))
                .andExpect(jsonPath("$.email").value(email));
        assertEquals("Petr", userRepository.findById(id)
                .orElseThrow().getName());
    }

    @Test
    void setActive_whenUserExists_deactivatesUser() throws Exception {
        long id = createUser();

        mockMvc.perform(patch("/api/users/{id}/active", id)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteById_whenUserExists_deletesUser() throws Exception {
        long id = createUser();

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(id));
    }

    private long createUser() throws Exception {
        String email = "user" + UUID.randomUUID() + "@example.com";
        String request = """
                {
                    "name": "Ivan",
                    "surname": "Ivanov",
                    "birthDate": "1999-09-29",
                    "email": "%s"
                }
                """.formatted(email);
        String response = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createUser(String name, String surname) throws Exception {
        String email = "user" + UUID.randomUUID() + "@example.com";
        String request = """
            {
                "name": "%s",
                "surname": "%s",
                "birthDate": "1999-09-29",
                "email": "%s"
            }
            """.formatted(name, surname, email);
        String response = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}