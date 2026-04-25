package com.example.comic.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieServiceTest {

    private AuthCookieService authCookieService;

    @BeforeEach
    void setUp() {
        authCookieService = new AuthCookieService();
        ReflectionTestUtils.setField(authCookieService, "cookieName", "COMIC_AUTH");
        ReflectionTestUtils.setField(authCookieService, "cookiePath", "/");
        ReflectionTestUtils.setField(authCookieService, "cookieSecure", false);
        ReflectionTestUtils.setField(authCookieService, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(authCookieService, "jwtExpirationMs", 120_000L);
    }

    @Test
    void buildTokenCookie_shouldContainConfiguredAttributes() {
        String cookie = authCookieService.buildTokenCookie("jwt-token");

        assertTrue(cookie.contains("COMIC_AUTH=jwt-token"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=120"));
        assertTrue(cookie.contains("SameSite=Lax"));
    }

    @Test
    void buildClearCookie_shouldSetZeroMaxAge() {
        String cookie = authCookieService.buildClearCookie();

        assertTrue(cookie.contains("COMIC_AUTH="));
        assertTrue(cookie.contains("Max-Age=0"));
    }

    @Test
    void resolveToken_shouldReturnMatchingCookieValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("OTHER", "abc"), new Cookie("COMIC_AUTH", "token-from-cookie"));

        String token = authCookieService.resolveToken(request);

        assertEquals("token-from-cookie", token);
    }

    @Test
    void resolveToken_shouldReturnNullForMissingOrBlankCookie() {
        MockHttpServletRequest requestWithBlank = new MockHttpServletRequest();
        requestWithBlank.setCookies(new Cookie("COMIC_AUTH", "   "));

        MockHttpServletRequest requestWithoutCookies = new MockHttpServletRequest();

        assertNull(authCookieService.resolveToken(requestWithBlank));
        assertNull(authCookieService.resolveToken(requestWithoutCookies));
    }

    @Test
    void resolveToken_shouldReturnNullForMatchingCookieWithNullValue() {
        Cookie cookie = Mockito.mock(Cookie.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(cookie.getName()).thenReturn("COMIC_AUTH");
        Mockito.when(cookie.getValue()).thenReturn(null);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        assertNull(authCookieService.resolveToken(request));
    }
}
