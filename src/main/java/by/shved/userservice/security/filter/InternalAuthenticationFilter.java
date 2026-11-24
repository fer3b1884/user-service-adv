package by.shved.userservice.security.filter;

import by.shved.userservice.config.property.InternalSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalAuthenticationFilter extends OncePerRequestFilter {
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String INTERNAL_AUTHORITY = "INTERNAL_SERVICE";

    private final InternalSecurityProperties properties;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String secretHeader = request.getHeader(INTERNAL_SECRET_HEADER);
        if (secretHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!isValidSecret(secretHeader)) {
            rejectAuthentication(request, response, "Invalid Authorization scheme");
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                "api-gateway",
                null,
                List.of(new SimpleGrantedAuthority(INTERNAL_AUTHORITY))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Internal service authenticated: {} {}", request.getMethod(), request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private boolean isValidSecret(String providedSecret) {
        return MessageDigest.isEqual(
                properties.secret().getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void rejectAuthentication(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(reason));
    }
}
