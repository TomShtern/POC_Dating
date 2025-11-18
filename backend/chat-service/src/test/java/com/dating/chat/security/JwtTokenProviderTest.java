package com.dating.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testSecret;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        testSecret = "test-secret-key-for-testing-must-be-32-chars";
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
    }

    @Test
    void validateToken_shouldReturnClaimsForValidToken() {
        String userId = UUID.randomUUID().toString();
        String username = "testuser";
        String token = generateTestToken(userId, username, 1);

        Claims claims = jwtTokenProvider.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("username", String.class)).isEqualTo(username);
    }

    @Test
    void validateToken_shouldThrowExceptionForExpiredToken() {
        String userId = UUID.randomUUID().toString();
        String token = generateTestToken(userId, "testuser", -1); // Expired

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void validateToken_shouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(invalidToken))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    void isValidToken_shouldReturnTrueForValidToken() {
        String token = generateTestToken(UUID.randomUUID().toString(), "testuser", 1);

        boolean result = jwtTokenProvider.isValidToken(token);

        assertThat(result).isTrue();
    }

    @Test
    void isValidToken_shouldReturnFalseForExpiredToken() {
        String token = generateTestToken(UUID.randomUUID().toString(), "testuser", -1);

        boolean result = jwtTokenProvider.isValidToken(token);

        assertThat(result).isFalse();
    }

    @Test
    void isValidToken_shouldReturnFalseForInvalidToken() {
        boolean result = jwtTokenProvider.isValidToken("invalid.token");

        assertThat(result).isFalse();
    }

    @Test
    void getUserIdFromToken_shouldExtractUserId() {
        UUID userId = UUID.randomUUID();
        String token = generateTestToken(userId.toString(), "testuser", 1);

        UUID result = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void getUsernameFromToken_shouldExtractUsername() {
        String username = "testuser";
        String token = generateTestToken(UUID.randomUUID().toString(), username, 1);

        String result = jwtTokenProvider.getUsernameFromToken(token);

        assertThat(result).isEqualTo(username);
    }

    private String generateTestToken(String userId, String username, int hoursFromNow) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("username", username)
                .claim("email", username + "@test.com")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(hoursFromNow, ChronoUnit.HOURS)))
                .setIssuer("dating-app")
                .signWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                .compact();
    }
}
