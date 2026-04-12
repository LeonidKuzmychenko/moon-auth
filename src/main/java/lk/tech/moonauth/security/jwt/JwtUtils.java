package lk.tech.moonauth.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lk.tech.moonauth.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("tokenType", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(key)
                .compact();
    }

    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getEmailFromToken(Claims claims) {
        return claims.getSubject();
    }

    public UUID getUserIdFromToken(Claims claims) {
        return UUID.fromString(claims.get("authId", String.class));
    }
}
