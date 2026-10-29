package by.shved.userservice.controller;

import by.shved.userservice.repository.PaymentCardRepository;
import com.redis.testcontainers.RedisContainer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
@AutoConfigureMockMvc
@SpringBootTest
class PaymentCardControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PaymentCardRepository paymentCardRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String adminToken;

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    @Container
    @ServiceConnection
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @BeforeEach
    void setUp() {
        adminToken = createAccessToken(1L, "ROLE_ADMIN");
    }

    @Test
    void create_whenRequestIsValid_returnsCreatedCard() throws Exception {
        long userId = createUser();
        String cardRequest = """
                {
                    "userId": %d,
                    "number": "4111111111111111",
                    "holder": "IVAN IVANOV",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(userId);

        mockMvc.perform(post("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.number").value("4111111111111111"))
                .andExpect(jsonPath("$.holder").value("IVAN IVANOV"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_whenCardNumberIsBlank_returnsBadRequest() throws Exception {
        long userId = createUser();
        String request = """
                {
                    "userId": %d,
                    "number": " ",
                    "holder": "IVAN IVANOV",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(userId);

        mockMvc.perform(post("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenUserDoesNotExist_returnsNotFound() throws Exception {
        String cardRequest = """
                {
                    "userId": %d,
                    "number": "4111111111111111",
                    "holder": "IVAN IVANOV",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(Long.MAX_VALUE);

        mockMvc.perform(post("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_whenUserAlreadyHasFiveCards_rejectsSixthCard() throws Exception {
        long userId = createUser();
        for (int i = 0; i < 5; i++) {
            createCard(userId);
        }
        String cardRequest = """
                {
                    "userId": %d,
                    "number": "4111111111111111",
                    "holder": "IVAN IVANOV",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(userId);

        mockMvc.perform(post("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 201",
            "ROLE_USER, 403"
    })
    void create_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long userId = createUser();
        String token = createAccessToken(userId, role);

        String request = """
            {
                "userId": %d,
                "number": "4111111111111111",
                "holder": "IVAN IVANOV",
                "expirationDate": "2030-12-31"
            }
            """.formatted(userId);

        mockMvc.perform(post("/api/payment-cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getById_whenCardExists_returnsCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(get("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getById_whenCardDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/payment-cards/{id}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void getById_checksAccessByRole(String role, boolean ownCard, int expectedStatus) throws Exception {
        long ownerId = createUser();
        long anotherUserId = createUser();
        long cardId = createCard(ownerId);
        long tokenUserId = ownCard ? ownerId : anotherUserId;
        String token = createAccessToken(tokenUserId, role);

        mockMvc.perform(get("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getById_whenTokenIsMissing_returnsUnauthorized() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(get("/api/payment-cards/{id}", cardId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_whenTokenIsInvalid_returnsUnauthorized() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(get("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer invalid token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllByUserId_whenUserHasCards_returnsUserCards() throws Exception {
        long userId = createUser();
        createCard(userId);

        mockMvc.perform(get("/api/payment-cards/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void getAllByUserId_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long ownerId = createUser();
        long anotherUserId = createUser();
        long tokenUserId = ownProfile ? ownerId : anotherUserId;
        String token = createAccessToken(tokenUserId, role);

        mockMvc.perform(get("/api/payment-cards/user/{userId}", ownerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getAll_whenUserNameIsProvided_filtersCardsByUserName() throws Exception {
        long ivanId = createUser("Mikhail", "Abramovich");
        long petrId = createUser("Stanislav", "Kovalev");
        createCard(ivanId);
        createCard(petrId);

        mockMvc.perform(get("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userName", "Mikhail")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(ivanId));
    }

    @Test
    void getAll_whenUserSurnameIsProvided_filtersCardsByUserSurname() throws Exception {
        long ivanId = createUser("Vladislav", "Chernov");
        long petrId = createUser("Artem", "Zhuravlev");
        createCard(ivanId);
        createCard(petrId);

        mockMvc.perform(get("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userSurname", "Zhuravlev")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(petrId));
    }

    @Test
    void getAll_whenNameAndSurnameAreProvided_filtersCardsByBothFields() throws Exception {
        long matchingUserId = createUser("Igor", "Vernik");
        long sameNameUserId = createUser("Igor", "Sidorov");
        long sameSurnameUserId = createUser("Lesha", "Vernik");

        createCard(matchingUserId);
        createCard(sameNameUserId);
        createCard(sameSurnameUserId);

        mockMvc.perform(get("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userName", "Igor")
                        .param("userSurname", "Vernik")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(matchingUserId));
    }

    @Test
    void getAll_whenNoUserMatchesFilters_returnsEmptyPage() throws Exception {
        long userId = createUser("Yaroslav", "Melnikov");
        createCard(userId);

        mockMvc.perform(get("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userName", "Qwerty")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 200",
            "ROLE_USER, 403"
    })
    void getAll_checksAccessByRole(String role, int expectedStatus) throws Exception {
        String token = createAccessToken(1L, role);

        mockMvc.perform(get("/api/payment-cards")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void update_whenCardExists_updatesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String request = """
                {
                    "number": "5555555555554444",
                    "holder": "PETR PETROV",
                    "expirationDate": "2031-12-31"
                }
                """;

        mockMvc.perform(put("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.number").value("5555555555554444"))
                .andExpect(jsonPath("$.holder").value("PETR PETROV"));
    }

    @Test
    void update_whenCardNumberIsBlank_returnsBadRequest() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String request = """
            {
                "number": " ",
                "holder": "IVAN IVANOV",
                "expirationDate": "2031-12-31"
            }
            """;

        mockMvc.perform(put("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenExpirationDateIsMissing_returnsBadRequest() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String request = """
            {
                "number": "5555555555554444",
                "holder": "IVAN IVANOV"
            }
            """;

        mockMvc.perform(put("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void update_checksAccessByRole(String role, boolean isOwner, int expectedStatus) throws Exception {
        long ownerId = createUser();
        long anotherUserId = createUser();
        long cardId = createCard(ownerId);
        long tokenUserId = isOwner ? ownerId : anotherUserId;
        String token = createAccessToken(tokenUserId, role);
        String request = """
            {
                "number": "5555555555554444",
                "holder": "PETR PETROV",
                "expirationDate": "2031-12-31"
            }
            """;

        mockMvc.perform(put("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void setActive_whenCardExists_deactivatesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(patch("/api/payment-cards/{id}/active", cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 200",
            "ROLE_USER, 403"
    })
    void setActive_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String token = createAccessToken(userId, role);

        mockMvc.perform(patch("/api/payment-cards/{id}/active", cardId)
                        .header("Authorization", "Bearer " + token)
                        .param("active", "false"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void deleteById_whenCardExists_deletesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(delete("/api/payment-cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertFalse(paymentCardRepository.existsById(cardId));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 204",
            "ROLE_USER, 403"
    })
    void deleteById_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String token = createAccessToken(userId, role);

        mockMvc.perform(delete("/api/payment-cards/{id}", cardId)
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

    private long createCard(long userId) throws Exception {
        String cardRequest = """
                {
                    "userId": %d,
                    "number": "4111111111111111",
                    "holder": "IVAN IVANOV",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(userId);
        String response = mockMvc.perform(post("/api/payment-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest))
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