package by.shved.userservice.controller;

import by.shved.userservice.repository.PaymentCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Test
    void create_whenRequestIsValid_returnsCreatedCard() throws Exception {
        long userId = createUser();
        String number = uniqueCardNumber();

        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(number, "IVAN IVANOV")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.holder").value("IVAN IVANOV"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @ParameterizedTest
    @CsvSource({
            "'', IVAN IVANOV",
            "'   ', IVAN IVANOV",
            "4111111111111111, ''",
            "4111111111111111, '   '"
    })
    void create_whenNumberOrHolderIsBlank_returnsBadRequest(String number, String holder) throws Exception {
        long userId = createUser();

        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(number, holder)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenExpirationDateIsMissing_returnsBadRequest() throws Exception {
        long userId = createUser();
        String request = """
                {
                    "number": "%s",
                    "holder": "IVAN IVANOV"
                }
                """.formatted(uniqueCardNumber());

        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenUserDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(uniqueCardNumber(), "IVAN IVANOV")))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_whenUserAlreadyHasFiveCards_rejectsSixthCard() throws Exception {
        long userId = createUser();
        for (int i = 0; i < 5; i++) {
            createCard(userId);
        }

        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(uniqueCardNumber(), "IVAN IVANOV")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 201",
            "ROLE_USER, true, 201",
            "ROLE_USER, false, 403"
    })
    void create_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long userId = createUser();
        long targetUserId = ownProfile ? userId : createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(uniqueCardNumber(), "IVAN IVANOV")))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void create_whenUserIsInactive_returnsForbidden() throws Exception {
        long userId = createUser();
        String token = createAccessToken(userId, "ROLE_USER");

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(uniqueCardNumber(), "IVAN IVANOV")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_whenCardExists_returnsCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(get(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getById_whenCardDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get(PAYMENT_CARDS_URL + "/{id}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, false, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void getById_checksAccessByRole(String role, boolean ownCard, int expectedStatus) throws Exception {
        long ownerId = createUser();
        long anotherUserId = createUser();
        long cardId = createCard(ownerId);
        long tokenUserId = ownCard ? ownerId : anotherUserId;
        String token = createAccessToken(tokenUserId, role);

        mockMvc.perform(get(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getAllByUserId_whenUserHasCards_returnsUserCards() throws Exception {
        long userId = createUser();
        createCard(userId);

        mockMvc.perform(get(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void getAllByUserId_whenUserHasNoCards_returnsEmptyList() throws Exception {
        long userId = createUser();

        mockMvc.perform(get(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, false, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void getAllByUserId_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long ownerId = createUser();
        long tokenUserId = ownProfile ? ownerId : createUser();
        String token = createAccessToken(tokenUserId, role);

        mockMvc.perform(get(PAYMENT_CARDS_URL + "/user/{userId}", ownerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getAll_whenUserNameIsProvided_filtersCardsByUserName() throws Exception {
        long ivanId = createUser("Mikhail", "Abramovich");
        long petrId = createUser("Stanislav", "Kovalev");
        createCard(ivanId);
        createCard(petrId);

        mockMvc.perform(get(PAYMENT_CARDS_URL)
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

        mockMvc.perform(get(PAYMENT_CARDS_URL)
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

        mockMvc.perform(get(PAYMENT_CARDS_URL)
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
    void getAll_whenNoCardsMatchFilters_returnsEmptyPage() throws Exception {
        long userId = createUser("Yaroslav", "Melnikov");
        createCard(userId);

        mockMvc.perform(get(PAYMENT_CARDS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userName", "NoSuchUser")
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
        String token = createAccessToken(adminProperties.userId(), role);

        mockMvc.perform(get(PAYMENT_CARDS_URL)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void update_whenCardExists_updatesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String number = uniqueCardNumber();

        mockMvc.perform(put(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(number, "PETR PETROV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.holder").value("PETR PETROV"));
    }

    @Test
    void update_whenCardNumberIsBlank_returnsBadRequest() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(put(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(" ", "IVAN IVANOV")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenExpirationDateIsMissing_returnsBadRequest() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String request = """
                {
                    "number": "%s",
                    "holder": "IVAN IVANOV"
                }
                """.formatted(uniqueCardNumber());

        mockMvc.perform(put(PAYMENT_CARDS_URL + "/{id}", cardId)
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

        mockMvc.perform(put(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(uniqueCardNumber(), "PETR PETROV")))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void update_whenOwnerIsInactive_returnsForbidden() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String token = createAccessToken(userId, "ROLE_USER");

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(put(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(uniqueCardNumber(), "PETR PETROV")))
                .andExpect(status().isForbidden());
    }

    @Test
    void setActive_whenCardExists_deactivatesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(patch(PAYMENT_CARDS_URL + "/{id}/active", cardId)
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

        mockMvc.perform(patch(PAYMENT_CARDS_URL + "/{id}/active", cardId)
                        .header("Authorization", "Bearer " + token)
                        .param("active", "false"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void deleteById_whenCardExists_deletesCard() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);

        mockMvc.perform(delete(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertFalse(paymentCardRepository.existsById(cardId));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, true, 204",
            "ROLE_USER, true, 204",
            "ROLE_USER, false, 403"
    })
    void deleteById_checksAccessByRole(String role, boolean isOwner, int expectedStatus) throws Exception {
        long ownerId = createUser();
        long anotherUserId = createUser();
        long cardId = createCard(ownerId);
        long tokenUserId = isOwner ? ownerId : anotherUserId;
        String token = createAccessToken(tokenUserId, role);

        mockMvc.perform(delete(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void deleteById_whenOwnerIsInactive_returnsForbidden() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String token = createAccessToken(userId, "ROLE_USER");

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(delete(PAYMENT_CARDS_URL + "/{id}", cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GET_ALL",
            "GET_BY_ID",
            "GET_BY_USER_ID",
            "CREATE",
            "UPDATE",
            "SET_ACTIVE",
            "DELETE"
    })
    void internalService_whenAccessingCardEndpoint_returnsForbidden(String endpoint) throws Exception {
        long userId = createUser();
        long cardId = createCard(userId);
        String cardRequest = cardRequest(uniqueCardNumber(), "IVAN IVANOV");
        var request = switch (endpoint) {
            case "GET_ALL" -> get(PAYMENT_CARDS_URL);
            case "GET_BY_ID" -> get(PAYMENT_CARDS_URL + "/{id}", cardId);
            case "GET_BY_USER_ID" -> get(PAYMENT_CARDS_URL + "/user/{userId}", userId);
            case "CREATE" -> post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cardRequest);
            case "UPDATE" -> put(PAYMENT_CARDS_URL + "/{id}", cardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cardRequest);
            case "SET_ACTIVE" -> patch(PAYMENT_CARDS_URL + "/{id}/active", cardId)
                    .param("active", "false");
            case "DELETE" -> delete(PAYMENT_CARDS_URL + "/{id}", cardId);
            default -> throw new IllegalArgumentException("Unsupported endpoint: " + endpoint);
        };

        mockMvc.perform(request
                        .header("X-Internal-Secret", internalSecurityProperties.secret()))
                .andExpect(status().isForbidden());
    }
}