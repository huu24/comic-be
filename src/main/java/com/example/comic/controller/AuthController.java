package com.example.comic.controller;

import com.example.comic.annotation.RateLimit;
import com.example.comic.model.User;
import com.example.comic.model.dto.AuthResponse;
import com.example.comic.model.dto.AuthMeResponse;
import com.example.comic.model.dto.LoginRequest;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.RegisterRequest;
import com.example.comic.model.dto.ResendEmailOtpRequest;
import com.example.comic.model.dto.VerifyEmailOtpRequest;
import com.example.comic.security.AuthCookieService;
import com.example.comic.service.AuthService;
import com.example.comic.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(AuthMeResponse.builder()
                    .role(currentUserService.resolveRole().name())
                    .build());
        }
        return ResponseEntity.ok(AuthMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build());
    }

    @PostMapping("/register")
    @RateLimit(
            limit = 5,
            duration = 1,
            unit = TimeUnit.MINUTES
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Set-Cookie", authCookieService.buildTokenCookie(response.getToken()))
                .body(response);
    }

    @PostMapping("/register-otp")
    public ResponseEntity<MessageResponse> registerWithOtp(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerWithOtp(request));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<AuthResponse> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        AuthResponse response = authService.verifyEmailOtp(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", authCookieService.buildTokenCookie(response.getToken()))
                .body(response);
    }

    @PostMapping("/resend-email-otp")
    public ResponseEntity<MessageResponse> resendEmailOtp(@Valid @RequestBody ResendEmailOtpRequest request) {
        return ResponseEntity.ok(authService.resendEmailOtp(request));
    }

    @PostMapping("/login")
    @RateLimit(
            limit = 5,
            duration = 1,
            unit = TimeUnit.MINUTES
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", authCookieService.buildTokenCookie(response.getToken()))
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
        HttpServletRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        String cookieToken = authCookieService.resolveToken(request);
        MessageResponse body = authService.logout(authorizationHeader, cookieToken);
        return ResponseEntity.ok().header("Set-Cookie", authCookieService.buildClearCookie()).body(body);
    }
}
