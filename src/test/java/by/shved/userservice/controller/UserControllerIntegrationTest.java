package by.shved.userservice.controller;

import by.shved.userservice.repository.UserRepository;
import com.redis.testcontainers.RedisContainer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import javax.crypto.SecretKey;
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
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String adminToken;

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

    @BeforeEach
    void setUp() {
        adminToken = createAccessToken(1L, "ROLE_ADMIN");
    }

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

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
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

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenEmailAlreadyExists_returnsConflict() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        createUser("Ivan", "Ivanov", email);
        String request = """
            {
                "name": "Petr",
                "surname": "Petrov",
                "birthDate": "1999-09-29",
                "email": "%s"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 201",
            "ROLE_USER, 403"
    })
    void create_checksAccessByRole(String role, int expectedStatus) throws Exception {
        String token = createAccessToken(1L, role);
        String email = "test" + UUID.randomUUID() + "@example.com";
        String request = """
            {
                "name": "Ivan",
                "surname": "Ivanov",
                "birthDate": "1999-09-29",
                "email": "%s"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getById_whenUserExists_returnsUser() throws Exception {
        long id = createUser();

        mockMvc.perform(get("/api/users/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Ivanov"));
    }

    @Test
    void getById_whenUserDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_whenUserRequestsOwnProfile_returnsOk() throws Exception {
        long id = createUser();
        String token = createAccessToken(id, "ROLE_USER");

        mockMvc.perform(get("/api/users/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getById_whenUserRequestsAnotherProfile_returnsForbidden() throws Exception {
        long firstUserId = createUser();
        long secondUserId = createUser();
        String token = createAccessToken(firstUserId, "ROLE_USER");

        mockMvc.perform(get("/api/users/{id}", secondUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void getById_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long userId = createUser();
        long targetId = ownProfile ? userId : createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(get("/api/users/{id}", targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getById_whenTokenIsMissing_returnsUnauthorized() throws Exception {
        long id = createUser();

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_whenTokenIsInvalid_returnsUnauthorized() throws Exception {
        long id = createUser();

        mockMvc.perform(get("/api/users/{id}", id)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_whenNameIsProvided_filtersUsersByName() throws Exception {
        createUser("Mikhail", "Abramovich");
        createUser("Stanislav", "Kovalev");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
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
        createUser("Vladislav", "Chernov");
        createUser("Artem", "Zhuravlev");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
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
        createUser("Igor", "Vernik");
        createUser("Igor", "Sidorov");
        createUser("Lesha", "Vernik");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("name", "Igor")
                        .param("surname", "Vernik")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Igor"))
                .andExpect(jsonPath("$.content[0].surname").value("Vernik"));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 200",
            "ROLE_USER, 403"
    })
    void getAll_checksAccessByRole(String role, int expectedStatus) throws Exception {
        String token = createAccessToken(1L, role);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getAll_whenAuthorizationSchemeIsInvalid_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Basic abc123"))
                .andExpect(status().isUnauthorized());
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

        mockMvc.perform(put("/api/users/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Petr"))
                .andExpect(jsonPath("$.surname").value("Petrov"))
                .andExpect(jsonPath("$.email").value(email));

        var updatedUser = userRepository.findById(id).orElseThrow();
        assertEquals("Petr", updatedUser.getName());
        assertEquals("Petrov", updatedUser.getSurname());
        assertEquals(email, updatedUser.getEmail());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void update_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long userId = createUser();
        long targetId = ownProfile ? userId : createUser();
        String token = createAccessToken(userId, role);
        String email = "updated" + UUID.randomUUID() + "@example.com";
        String request = """
            {
                "name": "Petr",
                "surname": "Petrov",
                "birthDate": "2000-05-15",
                "email": "%s"
            }
            """.formatted(email);

        mockMvc.perform(put("/api/users/{id}", targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void setActive_whenUserExists_deactivatesUser() throws Exception {
        long id = createUser();

        mockMvc.perform(patch("/api/users/{id}/active", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.active").value(false));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 200",
            "ROLE_USER, 403"
    })
    void setActive_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long id = createUser();
        String token = createAccessToken(id, role);

        mockMvc.perform(patch("/api/users/{id}/active", id)
                        .header("Authorization", "Bearer " + token)
                        .param("active", "false"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void deleteById_whenUserExists_deletesUser() throws Exception {
        long id = createUser();

        mockMvc.perform(delete("/api/users/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(id));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 204",
            "ROLE_USER, 403"
    })
    void deleteById_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long id = createUser();
        String token = createAccessToken(id, role);

        mockMvc.perform(delete("/api/users/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
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
        String response = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
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
        String response = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createUser(String name, String surname, String email) throws Exception {
        String request = """
            {
                "name": "%s",
                "surname": "%s",
                "birthDate": "1999-09-29",
                "email": "%s"
            }
            """.formatted(name, surname, email);
        String response = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String createAccessToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("tokenType", "ACCESS")
                .signWith(key)
                .compact();
    }
}