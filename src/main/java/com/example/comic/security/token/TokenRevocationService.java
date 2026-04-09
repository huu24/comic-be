package com.example.comic.security.token;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenRevocationService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public boolean isRevoked(String token) {
        cleanup();
        Instant expiry = revokedTokens.get(token);
        return expiry != null && expiry.isAfter(Instant.now());
    }

    public void revoke(String token, Date expiry) {
        if (token == null || token.isBlank() || isRevoked(token)) {
            return;
        }
        revokedTokens.put(token, expiry.toInstant());
        cleanup();
    }

    private void cleanup() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isBefore(now));
    }
}
