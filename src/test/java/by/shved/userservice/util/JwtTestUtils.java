package by.shved.userservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtTestUtils {
    public static String createAccessToken(String base64Secret, Long userId, String role) {
        Instant now = Instant.now();
        return createToken(
                base64Secret,
                String.valueOf(userId),
                Map.of(
                        "role", role,
                        "tokenType", "ACCESS"
                ),
                now,
                now.plusSeconds(3600)
        );
    }

    public static String createToken(
            String base64Secret,
            String subject,
            Map<String, Object> claims,
            Instant issuedAt,
            Instant expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        var builder = Jwts.builder();
        if (subject != null) {
            builder.subject(subject);
        }
        claims.forEach(builder::claim);
        if (issuedAt != null) {
            builder.issuedAt(Date.from(issuedAt));
        }
        if (expiresAt != null) {
            builder.expiration(Date.from(expiresAt));
        }
        return builder
                .signWith(key)
                .compact();
    }
}
