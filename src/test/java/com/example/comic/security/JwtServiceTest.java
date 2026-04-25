package com.example.comic.security;

import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "VGhpc0lzQVNlY3VyZVNlY3JldEtleUZvckpXVFNpZ25pbmdTdXBwb3J0MTIzNDU2");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Test
    void generateToken_shouldExtractUsernameAndExpiration() {
        UserDetails user = User.withUsername("user@example.com").password("pwd").authorities("ROLE_MEMBER").build();

        String token = jwtService.generateToken(user);

        assertEquals("user@example.com", jwtService.extractUsername(token));
        Date expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void generateTokenWithClaims_shouldExposeCustomClaim() {
        UserDetails user = User.withUsername("user@example.com").password("pwd").authorities("ROLE_MEMBER").build();

        String token = jwtService.generateToken(Map.of("provider", "google"), user);

        String provider = jwtService.extractClaim(token, claims -> (String) claims.get("provider"));
        assertEquals("google", provider);
    }

    @Test
    void isTokenValid_shouldMatchUsername() {
        UserDetails user = User.withUsername("user@example.com").password("pwd").authorities("ROLE_MEMBER").build();
        UserDetails anotherUser = User.withUsername("other@example.com").password("pwd").authorities("ROLE_MEMBER").build();
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        JwtService controllableJwtService = new JwtService() {
            @Override
            public String extractUsername(String token) {
                return "user@example.com";
            }

            @Override
            public Date extractExpiration(String token) {
                return new Date(System.currentTimeMillis() - 1_000);
            }
        };
        UserDetails user = User.withUsername("user@example.com").password("pwd").authorities("ROLE_MEMBER").build();

        assertFalse(controllableJwtService.isTokenValid("expired", user));
    }
}
