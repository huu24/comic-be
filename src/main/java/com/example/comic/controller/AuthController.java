package com.example.comic.controller;

import com.example.comic.model.dto.AuthResponse;
import com.example.comic.model.dto.LoginRequest;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.RegisterRequest;
import com.example.comic.model.dto.ResendEmailOtpRequest;
import com.example.comic.model.dto.VerifyEmailOtpRequest;
import com.example.comic.security.AuthCookieService;
import com.example.comic.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<AuthResponse> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(authService.verifyEmailOtp(request));
    }

    @PostMapping("/resend-email-otp")
    public ResponseEntity<MessageResponse> resendEmailOtp(@Valid @RequestBody ResendEmailOtpRequest request) {
        return ResponseEntity.ok(authService.resendEmailOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
