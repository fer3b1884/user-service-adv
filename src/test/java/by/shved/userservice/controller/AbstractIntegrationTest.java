package by.shved.userservice.controller;

import by.shved.userservice.config.property.AdminProperties;
import by.shved.userservice.config.property.InternalSecurityProperties;
import by.shved.userservice.config.property.JwtProperties;
import by.shved.userservice.util.JwtTestUtils;
import by.shved.userservice.util.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {
    protected static final String USERS_URL = "/api/v1/users";
    protected static final String PAYMENT_CARDS_URL = "/api/v1/payment-cards";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JwtProperties jwtProperties;
    @Autowired
    protected AdminProperties adminProperties;
    @Autowired
    protected InternalSecurityProperties internalSecurityProperties;

    protected String adminToken;
    protected String internalSecret;

    @BeforeEach
    void setUpAdminTokenAndInternalSecret() {
        adminToken = createAccessToken(adminProperties.userId(), "ROLE_ADMIN");
        internalSecret = internalSecurityProperties.secret();
    }

    protected String createAccessToken(Long userId, String role) {
        return JwtTestUtils.createAccessToken(jwtProperties.secret(), userId, role);
    }

    protected long createUser() throws Exception {
        return createUser("Ivan", "Ivanov");
    }

    protected long createUser(String name, String surname) throws Exception {
        return createUser(name, surname, uniqueEmail());
    }

    protected long createUser(String name, String surname, String email) throws Exception {
        String response = mockMvc.perform(post(USERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest(name, surname, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    protected long createCard(long userId) throws Exception {
        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post(PAYMENT_CARDS_URL + "/user/{userId}", userId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardRequest(uniqueCardNumber(), "IVAN IVANOV"))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected String uniqueCardNumber() {
        return "4" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 15);
    }

    protected String userRequest(String name, String surname, String email) {
        return """
                {
                    "name": "%s",
                    "surname": "%s",
                    "birthDate": "1999-09-29",
                    "email": "%s"
                }
                """.formatted(name, surname, email);
    }

    protected String cardRequest(String number, String holder) {
        return """
                {
                    "number": "%s",
                    "holder": "%s",
                    "expirationDate": "2030-12-31"
                }
                """.formatted(number, holder);
    }
}
