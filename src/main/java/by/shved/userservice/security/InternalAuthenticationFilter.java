package by.shved.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
@Component
public class InternalAuthenticationFilter extends OncePerRequestFilter {
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String INTERNAL_AUTHORITY = "INTERNAL_SERVICE";

    private final String internalSecret;

    public InternalAuthenticationFilter(@Value("${app.internal.secret}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String secretHeader = request.getHeader(INTERNAL_SECRET_HEADER);
        if (secretHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!isValidSecret(secretHeader)) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                "api-gateway",
                null,
                List.of(new SimpleGrantedAuthority(INTERNAL_AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Internal service authenticated: {} {}", request.getMethod(), request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private boolean isValidSecret(String providedSecret) {
        return MessageDigest.isEqual(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}