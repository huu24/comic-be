package com.example.comic.security.oauth2;

import com.example.comic.model.dto.AuthResponse;
import com.example.comic.security.AuthCookieService;
import com.example.comic.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Value("${application.security.oauth2.redirect-success-url:http://localhost:3000/oauth2/success}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = getStringClaim(oauth2User, "email");
        String name = getStringClaim(oauth2User, "name");
        String picture = getStringClaim(oauth2User, "picture");

        AuthResponse authResponse = authService.authenticateGoogleUser(email, name, picture);
        response.addHeader("Set-Cookie", authCookieService.buildTokenCookie(authResponse.getToken()));
        response.sendRedirect(successRedirectUrl);
    }

    private String getStringClaim(OAuth2User oauth2User, String key) {
        Object value = oauth2User.getAttributes().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
