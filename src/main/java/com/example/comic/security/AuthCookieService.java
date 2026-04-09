package com.example.comic.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieService {

    @Value("${application.security.jwt.cookie-name:COMIC_AUTH}")
    private String cookieName;

    @Value("${application.security.jwt.cookie-path:/}")
    private String cookiePath;

    @Value("${application.security.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${application.security.jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    @Value("${application.security.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public String buildTokenCookie(String token) {
        long maxAgeSeconds = Math.max(1, jwtExpirationMs / 1000);
        return ResponseCookie
            .from(cookieName, token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path(cookiePath)
            .sameSite(cookieSameSite)
            .maxAge(maxAgeSeconds)
            .build()
            .toString();
    }

    public String buildClearCookie() {
        return ResponseCookie
            .from(cookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path(cookiePath)
            .sameSite(cookieSameSite)
            .maxAge(0)
            .build()
            .toString();
    }

    public String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        return null;
    }
}
