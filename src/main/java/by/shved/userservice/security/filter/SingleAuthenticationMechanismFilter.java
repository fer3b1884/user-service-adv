package by.shved.userservice.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SingleAuthenticationMechanismFilter extends OncePerRequestFilter {
    private static final Set<String> AUTH_HEADERS = Set.of(
            "Authorization",
            "X-Internal-Secret"
    );
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long presentHeadersCount = AUTH_HEADERS.stream()
                .filter(header -> request.getHeader(header) != null)
                .count();
        if (presentHeadersCount > 1) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Multiple authentication mechanisms provided")
            );
            return;
        }
        filterChain.doFilter(request, response);
    }
}
