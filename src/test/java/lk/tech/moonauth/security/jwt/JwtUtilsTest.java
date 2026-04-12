package lk.tech.moonauth.security.jwt;

import io.jsonwebtoken.Claims;
import lk.tech.moonauth.user.entity.Role;
import lk.tech.moonauth.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "v9y$B&E)H@McQfTjWnZr4u7x!A%D*G-KaNdRgUkXp2s5v8y/B?E(H+MbQeShVmYq}";
    private final long expiration = 600000;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(secret, expiration);
    }

    @Test
    void generateAndValidateToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.USER)
                .build();

        String token = jwtUtils.generateAccessToken(user);
        assertNotNull(token);

        Claims claims = jwtUtils.validateAccessToken(token);
        assertNotNull(claims);
        assertEquals("test@example.com", jwtUtils.getEmailFromToken(claims));
        assertEquals(user.getId(), jwtUtils.getUserIdFromToken(claims));
        assertEquals("USER", claims.get("role"));
    }

    @Test
    void validateInvalidToken() {
        assertNull(jwtUtils.validateAccessToken("invalid-token"));
    }
}
