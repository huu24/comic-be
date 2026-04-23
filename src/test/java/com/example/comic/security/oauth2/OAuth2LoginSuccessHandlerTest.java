package com.example.comic.security.oauth2;

import com.example.comic.model.dto.AuthResponse;
import com.example.comic.security.AuthCookieService;
import com.example.comic.service.AuthService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    private AuthService authService;
    private AuthCookieService authCookieService;
    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        authCookieService = Mockito.mock(AuthCookieService.class);
        successHandler = new OAuth2LoginSuccessHandler(authService, authCookieService);
        ReflectionTestUtils.setField(successHandler, "successRedirectUrl", "http://localhost:3000/oauth2/success");
    }

    @Test
    void onAuthenticationSuccess_shouldIssueCookieAndRedirect() throws Exception {
        OAuth2User oauth2User = new DefaultOAuth2User(
            java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("email", "google@example.com", "name", "Google User", "picture", "avatar-url"),
            "email"
        );
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(authService.authenticateGoogleUser("google@example.com", "Google User", "avatar-url"))
            .thenReturn(AuthResponse.builder().token("jwt-token").build());
        when(authCookieService.buildTokenCookie("jwt-token")).thenReturn("COMIC_AUTH=jwt-token; Path=/; HttpOnly");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(authService).authenticateGoogleUser(eq("google@example.com"), eq("Google User"), eq("avatar-url"));
        assertTrue(response.getHeader("Set-Cookie").contains("COMIC_AUTH=jwt-token"));
        assertEquals("http://localhost:3000/oauth2/success", response.getRedirectedUrl());
    }
}
