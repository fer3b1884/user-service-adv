package by.shved.userservice.security.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

class SingleAuthenticationMechanismFilterTest extends AbstractSecurityFilterTest {
    private SingleAuthenticationMechanismFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SingleAuthenticationMechanismFilter(authenticationEntryPoint);
    }

    @ParameterizedTest
    @CsvSource({
            ",",
            "Authorization, Bearer some-token",
            "X-Internal-Secret, internal-secret"
    })
    void doFilter_whenZeroOrOneAuthenticationMechanismProvided_continuesFilterChain(
            String headerName,
            String headerValue) throws Exception {
        if (headerName != null) {
            addHeader(headerName, headerValue);
        }

        executeFilter(filter);

        assertFilterChainContinued();
        verifyEntryPointNotInvoked();
    }

    @Test
    void doFilter_whenMultipleAuthenticationMechanismsProvided_rejectsRequest() throws Exception {
        addHeader("Authorization", "Bearer some-token");
        addHeader("X-Internal-Secret", "internal-secret");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        executeFilter(filter);

        assertFilterChainStopped();
        assertNoAuthentication();
        verifyEntryPointInvoked();
    }
}