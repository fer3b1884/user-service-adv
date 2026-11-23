package by.shved.userservice.security.filter;

import by.shved.userservice.config.property.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_USER"
    );

    private final JwtParser jwtParser;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtProperties jwtProperties, AuthenticationEntryPoint authenticationEntryPoint) {
        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            rejectAuthentication(request, response, "Invalid Authorization scheme");
            return;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            rejectAuthentication(request, response, "Bearer token is empty");
            return;
        }
        try {
            Claims claims = jwtParser
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            String tokenType = claims.get("tokenType", String.class);

            if (subject == null || role == null || tokenType == null) {
                rejectAuthentication(request, response, "Missing required JWT claims");
                return;
            }
            Long userId = Long.valueOf(subject);
            if (userId <= 0 || !"ACCESS".equals(tokenType) || !ALLOWED_ROLES.contains(role)) {
                rejectAuthentication(request, response, "Invalid JWT claims");
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority(role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT authenticated request: {} {}, userId={}, role={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    userId,
                    role
            );
        } catch (JwtException | IllegalArgumentException exception) {
            rejectAuthentication(request, response, "JWT validation failed");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void rejectAuthentication(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(reason));
    }
}
