package by.shved.userservice.security.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
abstract class AbstractSecurityFilterTest {
    @Mock
    protected AuthenticationEntryPoint authenticationEntryPoint;

    protected MockHttpServletRequest request;
    protected MockHttpServletResponse response;
    protected MockFilterChain filterChain;

    @BeforeEach
    void setUpRequest() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected void addHeader(String name, String value) {
        request.addHeader(name, value);
    }

    protected void executeFilter(Filter filter) throws Exception {
        filter.doFilter(request, response, filterChain);
    }

    protected void assertFilterChainContinued() {
        assertNotNull(filterChain.getRequest());
    }

    protected void assertFilterChainStopped() {
        assertNull(filterChain.getRequest());
    }

    protected void assertNoAuthentication() {
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    protected void verifyEntryPointInvoked() throws ServletException, IOException {
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
    }

    protected void verifyEntryPointNotInvoked() {
        verifyNoInteractions(authenticationEntryPoint);
    }
}
