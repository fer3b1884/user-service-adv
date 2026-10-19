package by.shved.userservice.controller;

import by.shved.userservice.repository.PaymentCardRepository;
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
@AutoConfigureMockMvc
@SpringBootTest
class PaymentCardControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PaymentCardRepository paymentCardRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    @Container
    @ServiceConnection
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

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

        mockMvc.perform(post("/api/payment-cards").contentType(MediaType.APPLICATION_JSON).content(cardRequest))
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

        mockMvc.perform(post("/api/payment-cards").contentType(MediaType.APPLICATION_JSON).content(request))
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

        mockMvc.perform(post("/api/payment-cards").contentType(MediaType.APPLICATION_JSON).content(cardRequest))
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

        mockMvc.perform(post("/api/payment-cards").contentType(MediaType.APPLICATION_JSON).content(cardRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_whenCardExists_returnsCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(get("/api/payment-cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getById_whenCardDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/payment-cards/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllByUserId_whenUserHasCards_returnsUserCards() throws Exception {
        long userId = createUser();
        createCard(userId);

        mockMvc.perform(get("/api/payment-cards/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void getAll_whenUserNameIsProvided_filtersCardsByUserName() throws Exception {
        long ivanId = createUser("Mikhail", "Abramovich");
        long petrId = createUser("Stanislav", "Kovalev");
        createCard(ivanId);
        createCard(petrId);

        mockMvc.perform(get("/api/payment-cards")
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
                        .param("userName", "Qwerty")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
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

        mockMvc.perform(put("/api/payment-cards/{id}", cardId).contentType(MediaType.APPLICATION_JSON).content(request))
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

        mockMvc.perform(put("/api/payment-cards/{id}", cardId).contentType(MediaType.APPLICATION_JSON).content(request))
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

        mockMvc.perform(put("/api/payment-cards/{id}", cardId).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setActive_whenCardExists_deactivatesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(patch("/api/payment-cards/{id}/active", cardId).param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteById_whenCardExists_deletesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(delete("/api/payment-cards/{id}", cardId))
                .andExpect(status().isNoContent());
        assertFalse(paymentCardRepository.existsById(cardId));
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

    private long createCard(long userId) throws Exception {
        String cardRequest = """
                {
                    "userId": %d,
                    "number": "4111111111111111",
                    "holder": "IVAN IVANOV",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(userId);
        String response = mockMvc.perform(post("/api/payment-cards").contentType(MediaType.APPLICATION_JSON).content(cardRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}