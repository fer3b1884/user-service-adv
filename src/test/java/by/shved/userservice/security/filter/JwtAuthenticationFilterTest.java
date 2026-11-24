package by.shved.userservice.security.filter;

import by.shved.userservice.config.property.JwtProperties;
import by.shved.userservice.util.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest extends AbstractSecurityFilterTest {
    private static final String JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String ANOTHER_JWT_SECRET = "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=";

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(new JwtProperties(JWT_SECRET), authenticationEntryPoint);
    }

    @Test
    void doFilter_whenAuthorizationHeaderIsMissing_continuesFilterChain() throws Exception {
        executeFilter(filter);

        assertFilterChainContinued();
        assertNoAuthentication();
        verifyEntryPointNotInvoked();
    }

    @ParameterizedTest
    @CsvSource({
            "1, ROLE_USER",
            "2, ROLE_ADMIN"
    })
    void doFilter_whenAccessTokenIsValid_authenticatesUser(Long userId, String role) throws Exception {
        String token = JwtTestUtils.createAccessToken(JWT_SECRET, userId, role);
        addHeader("Authorization", "Bearer " + token);

        executeFilter(filter);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(userId, authentication.getPrincipal());
        assertEquals(List.of(new SimpleGrantedAuthority(role)), authentication.getAuthorities());
        assertFilterChainContinued();
        verifyEntryPointNotInvoked();
    }

    static Stream<String> invalidAuthorizationHeaders() {
        Instant now = Instant.now();

        return Stream.of(
                "Basic some-token",
                "Bearer ",
                "Bearer invalid-token",
                "Bearer " + JwtTestUtils.createToken(
                        JWT_SECRET,
                        "1",
                        Map.of(
                                "role", "ROLE_USER",
                                "tokenType", "ACCESS"
                        ),
                        now.minusSeconds(3600),
                        now.minusSeconds(60)
                ),
                "Bearer " + JwtTestUtils.createAccessToken(
                        ANOTHER_JWT_SECRET,
                        1L,
                        "ROLE_USER"
                ),
                "Bearer " + JwtTestUtils.createToken(
                        JWT_SECRET,
                        "1",
                        Map.of(
                                "role", "ROLE_USER",
                                "tokenType", "REFRESH"
                        ),
                        now,
                        now.plusSeconds(3600)
                ),
                "Bearer " + JwtTestUtils.createToken(
                        JWT_SECRET,
                        "1",
                        Map.of(
                                "role", "ROLE_SUPERUSER",
                                "tokenType", "ACCESS"
                        ),
                        now,
                        now.plusSeconds(3600)
                ),
                "Bearer " + JwtTestUtils.createToken(
                        JWT_SECRET,
                        "1",
                        Map.of("tokenType", "ACCESS"),
                        now,
                        now.plusSeconds(3600)
                ),
                "Bearer " + JwtTestUtils.createToken(
                        JWT_SECRET,
                        "invalid-user-id",
                        Map.of(
                                "role", "ROLE_USER",
                                "tokenType", "ACCESS"
                        ),
                        now,
                        now.plusSeconds(3600)
                ),
                "Bearer " + JwtTestUtils.createAccessToken(
                        JWT_SECRET,
                        -1L,
                        "ROLE_USER"
                ),
                "Bearer " + JwtTestUtils.createAccessToken(
                        JWT_SECRET,
                        0L,
                        "ROLE_USER"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaders")
    void doFilter_whenAuthorizationHeaderIsInvalid_rejectsAuthentication(String authorizationHeader) throws Exception {
        addHeader("Authorization", authorizationHeader);

        executeFilter(filter);

        assertFilterChainStopped();
        assertNoAuthentication();
        verifyEntryPointInvoked();
    }
}