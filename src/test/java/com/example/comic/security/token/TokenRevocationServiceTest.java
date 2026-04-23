package com.example.comic.security.token;

import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

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
}
