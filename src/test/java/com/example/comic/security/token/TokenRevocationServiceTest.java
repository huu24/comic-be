package com.example.comic.security.token;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRevocationServiceTest {

    private final TokenRevocationService tokenRevocationService = new TokenRevocationService();

    @Test
    void revokeAndCheck_shouldMarkTokenAsRevokedWhenNotExpired() {
        String token = "abc.def.ghi";

        tokenRevocationService.revoke(token, Date.from(Instant.now().plusSeconds(60)));

        assertTrue(tokenRevocationService.isRevoked(token));
    }

    @Test
    void revoke_shouldIgnoreNullBlankAndExpiredTokens() {
        tokenRevocationService.revoke(null, Date.from(Instant.now().plusSeconds(60)));
        tokenRevocationService.revoke("   ", Date.from(Instant.now().plusSeconds(60)));
        tokenRevocationService.revoke("expired-token", Date.from(Instant.now().minusSeconds(60)));

        assertFalse(tokenRevocationService.isRevoked("expired-token"));
    }

    @Test
    void isRevoked_shouldCleanupExpiredEntries() {
        String token = "short-lived";
        tokenRevocationService.revoke(token, Date.from(Instant.now().plusMillis(5)));

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(tokenRevocationService.isRevoked(token));
    }

    @Test
    void revoke_shouldSkipWhenTokenAlreadyRevoked() {
        String token = "already-revoked";
        Date firstExpiry = Date.from(Instant.now().plusSeconds(120));
        Date secondExpiry = Date.from(Instant.now().plusSeconds(240));

        tokenRevocationService.revoke(token, firstExpiry);

        @SuppressWarnings("unchecked")
        Map<String, Instant> revokedTokens = (Map<String, Instant>) ReflectionTestUtils.getField(tokenRevocationService, "revokedTokens");
        Instant storedAfterFirstRevoke = revokedTokens == null ? null : revokedTokens.get(token);

        tokenRevocationService.revoke(token, secondExpiry);

        assertTrue(tokenRevocationService.isRevoked(token));
        assertTrue(revokedTokens != null && storedAfterFirstRevoke != null && storedAfterFirstRevoke.equals(revokedTokens.get(token)));
    }

    @Test
    void isRevoked_shouldCleanupNullExpiryEntries() {
        TokenRevocationService localService = new TokenRevocationService();
        String token = "null-expiry";
        Map<String, Instant> testMap = new HashMap<>();
        testMap.put(token, null);
        ReflectionTestUtils.setField(localService, "revokedTokens", testMap);

        @SuppressWarnings("unchecked")
        Map<String, Instant> revokedTokens = (Map<String, Instant>) ReflectionTestUtils.getField(localService, "revokedTokens");

        assertFalse(localService.isRevoked(token));
        assertTrue(revokedTokens == null || !revokedTokens.containsKey(token));
    }

    @Test
    void isRevoked_shouldReturnFalseWhenExpiryEqualsCurrentTime() {
        TokenRevocationService localService = new TokenRevocationService();
        String token = "edge-token";
        Instant cleanupNow = Instant.parse("2026-01-01T00:00:00Z");
        Instant checkNow = Instant.parse("2026-01-01T00:00:01Z");

        @SuppressWarnings("unchecked")
        Map<String, Instant> revokedTokens = (Map<String, Instant>) ReflectionTestUtils.getField(localService, "revokedTokens");
        if (revokedTokens != null) {
            revokedTokens.put(token, checkNow);
        }

        try (MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class)) {
            instantMock.when(Instant::now).thenReturn(cleanupNow, checkNow);

            assertFalse(localService.isRevoked(token));
        }
    }
}
