package by.shved.userservice.controller;

import by.shved.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIntegrationTest extends AbstractIntegrationTest{
    @Autowired
    private UserRepository userRepository;

    @Test
    void create_whenRequestIsValid_returnsCreatedUser() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post(USERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Ivan", "Ivanov", email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Ivanov"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.active").value(true));
        assertTrue(userRepository.existsByEmail(email));
    }

    @ParameterizedTest
    @CsvSource({
            "'', Ivanov",
            "'   ', Ivanov",
            "Ivan, ''",
            "Ivan, '   '"
    })
    void create_whenNameOrSurnameIsBlank_returnsBadRequest(String name, String surname) throws Exception {
        mockMvc.perform(post(USERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest(name, surname, uniqueEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenEmailAlreadyExists_returnsConflict() throws Exception {
        String email = uniqueEmail();
        createUser("Ivan", "Ivanov", email);

        mockMvc.perform(post(USERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Petr", "Petrov", email)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 201",
            "ROLE_USER, 403"
    })
    void create_checksAccessByRole(String role, int expectedStatus) throws Exception {
        String token = createAccessToken(adminProperties.userId(), role);

        mockMvc.perform(post(USERS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Ivan", "Ivanov", uniqueEmail())))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void create_whenInternalSecretIsValid_returnsCreatedUser() throws Exception {
        mockMvc.perform(post(USERS_URL)
                        .header("X-Internal-Secret", internalSecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Ivan", "Ivanov", uniqueEmail())))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_whenUserExists_returnsUser() throws Exception {
        long userId = createUser();

        mockMvc.perform(get(USERS_URL + "/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Ivanov"));
    }

    @Test
    void getById_whenUserDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get(USERS_URL + "/{id}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, false, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void getById_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long userId = createUser();
        long targetId = ownProfile ? userId : createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(get(USERS_URL + "/{id}", targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest
    @CsvSource({
            "missing, 401",
            "invalid, 401"
    })
    void getById_checksAuthentication(String tokenType, int expectedStatus) throws Exception {
        long userId = createUser();

        var request = get(USERS_URL + "/{id}", userId);

        if ("invalid".equals(tokenType)) {
            request.header("Authorization", "Bearer invalid-token");
        }

        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void getAll_whenNameIsProvided_filtersUsersByName() throws Exception {
        createUser("Mikhail", "Abramovich");
        createUser("Stanislav", "Kovalev");

        mockMvc.perform(get(USERS_URL)
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

        mockMvc.perform(get(USERS_URL)
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
    void getAll_whenNameAndSurnameAreProvided_filtersByBothFields() throws Exception {
        createUser("Igor", "Vernik");
        createUser("Igor", "Sidorov");
        createUser("Lesha", "Vernik");

        mockMvc.perform(get(USERS_URL)
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
        String token = createAccessToken(adminProperties.userId(), role);

        mockMvc.perform(get(USERS_URL)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void isUserActive_whenUserIsActive_returnsTrue() throws Exception {
        long userId = createUser();

        mockMvc.perform(get(USERS_URL + "/{id}/active", userId)
                        .header("X-Internal-Secret", internalSecret))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isUserActive_whenUserIsInactive_returnsFalse() throws Exception {
        long userId = createUser();

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get(USERS_URL + "/{id}/active", userId)
                        .header("X-Internal-Secret", internalSecret))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN",
            "ROLE_USER"
    })
    void isUserActive_rejectsJwtAuthentication(String role) throws Exception {
        long userId = createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(get(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_whenUserExists_updatesUser() throws Exception {
        long userId = createUser();
        String email = uniqueEmail();

        mockMvc.perform(put(USERS_URL + "/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Petr", "Petrov", email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Petr"))
                .andExpect(jsonPath("$.surname").value("Petrov"))
                .andExpect(jsonPath("$.email").value(email));

        var updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals("Petr", updatedUser.getName());
        assertEquals("Petrov", updatedUser.getSurname());
        assertEquals(email, updatedUser.getEmail());
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, false, 200",
            "ROLE_USER, true, 200",
            "ROLE_USER, false, 403"
    })
    void update_checksAccessByRole(String role, boolean ownProfile, int expectedStatus) throws Exception {
        long userId = createUser();
        long targetId = ownProfile ? userId : createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(put(USERS_URL + "/{id}", targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Petr", "Petrov", uniqueEmail())))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void update_whenUserIsInactive_returnsForbidden() throws Exception {
        long userId = createUser();
        String token = createAccessToken(userId, "ROLE_USER");

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(put(USERS_URL + "/{id}", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest("Petr", "Petrov", uniqueEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void setActive_whenUserExists_deactivatesUser() throws Exception {
        long userId = createUser();

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.active").value(false));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 200",
            "ROLE_USER, 403"
    })
    void setActive_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long userId = createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(patch(USERS_URL + "/{id}/active", userId)
                        .header("Authorization", "Bearer " + token)
                        .param("active", "false"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void deleteById_whenUserExists_deletesUser() throws Exception {
        long userId = createUser();

        mockMvc.perform(delete(USERS_URL + "/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(userId));
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN, 204",
            "ROLE_USER, 403"
    })
    void deleteById_checksAccessByRole(String role, int expectedStatus) throws Exception {
        long userId = createUser();
        String token = createAccessToken(userId, role);

        mockMvc.perform(delete(USERS_URL + "/{id}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void deleteById_whenInternalSecretIsValid_deletesUser() throws Exception {
        long userId = createUser();

        mockMvc.perform(delete(USERS_URL + "/{id}", userId)
                        .header("X-Internal-Secret", internalSecret))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(userId));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GET_ALL",
            "GET_BY_ID",
            "UPDATE",
            "SET_ACTIVE"
    })
    void internalService_whenAccessingForbiddenEndpoint_returnsForbidden(String endpoint) throws Exception {
        long userId = createUser();
        var request = switch (endpoint) {
            case "GET_ALL" -> get(USERS_URL);
            case "GET_BY_ID" -> get(USERS_URL + "/{id}", userId);
            case "UPDATE" -> put(USERS_URL + "/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userRequest("Petr", "Petrov", uniqueEmail()));
            case "SET_ACTIVE" -> patch(USERS_URL + "/{id}/active", userId)
                    .param("active", "false");
            default -> throw new IllegalArgumentException("Unsupported endpoint: " + endpoint);
        };

        mockMvc.perform(request
                        .header("X-Internal-Secret", internalSecurityProperties.secret()))
                .andExpect(status().isForbidden());
    }
}