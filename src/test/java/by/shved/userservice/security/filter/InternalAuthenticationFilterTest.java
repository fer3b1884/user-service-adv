package by.shved.userservice.security.filter;

import by.shved.userservice.config.property.InternalSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class InternalAuthenticationFilterTest extends AbstractSecurityFilterTest {
    private static final String SECRET = "test-internal-secret-with-length-more-than-32-chars";
    private InternalAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalAuthenticationFilter(new InternalSecurityProperties(SECRET), authenticationEntryPoint);
    }

    @Test
    void doFilter_whenSecretHeaderIsAbsent_continuesFilterChain() throws Exception {
        executeFilter(filter);

        assertFilterChainContinued();
        assertNoAuthentication();
        verifyEntryPointNotInvoked();
    }

    @Test
    void doFilter_whenSecretIsValid_authenticatesInternalService() throws Exception {
        addHeader("X-Internal-Secret", SECRET);

        executeFilter(filter);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("api-gateway", authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("INTERNAL_SERVICE")));
        assertFilterChainContinued();
        verifyEntryPointNotInvoked();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "wrong-secret",
            ""
    })
    void doFilter_whenSecretIsInvalid_rejectsAuthentication(String secret) throws Exception {
        addHeader("X-Internal-Secret", secret);

        executeFilter(filter);

        assertFilterChainStopped();
        assertNoAuthentication();
        verifyEntryPointInvoked();
    }
}