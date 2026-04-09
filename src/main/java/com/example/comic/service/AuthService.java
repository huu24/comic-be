package com.example.comic.service;

import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.User;
import com.example.comic.model.UserStatus;
import com.example.comic.model.dto.AuthResponse;
import com.example.comic.model.dto.AuthUserResponse;
import com.example.comic.model.dto.LoginRequest;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.RegisterRequest;
import com.example.comic.model.dto.ResendEmailOtpRequest;
import com.example.comic.model.dto.VerifyEmailOtpRequest;
import com.example.comic.repository.UserRepository;
import com.example.comic.security.JwtService;
import com.example.comic.security.token.TokenRevocationService;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final TokenRevocationService tokenRevocationService;
    private final EmailOtpService emailOtpService;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String fullName = request.getFullName() == null ? null : request.getFullName().trim();

        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            if (existingUser.getStatus() == UserStatus.ACTIVE) {
                throw new AlreadyExistsException("Email này đã được đăng ký trong hệ thống.");
            }

            if (existingUser.getStatus() == UserStatus.LOCKED) {
                throw new PermissionDeniedException("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
            }

            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existingUser.setFullName(fullName);
            existingUser.setStatus(UserStatus.PENDING_VERIFICATION);
            userRepository.save(existingUser);
            emailOtpService.issueOtp(email, fullName);

            return MessageResponse
                .builder()
                .message("Email đã đăng ký nhưng chưa xác thực. Hệ thống đã gửi lại OTP 6 số tới email của bạn.")
                .build();
        }

        User user = User
            .builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(fullName)
            .role("MEMBER")
            .status(UserStatus.PENDING_VERIFICATION)
            .build();

        userRepository.save(Objects.requireNonNull(user));
        emailOtpService.issueOtp(email, fullName);

        return MessageResponse
            .builder()
            .message("Đăng ký thành công. Vui lòng nhập OTP 6 số đã gửi tới email để kích hoạt tài khoản.")
            .build();
    }

    @Transactional
    public AuthResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthenticatedException("Tài khoản không tồn tại."));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new PermissionDeniedException("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            String token = jwtService.generateToken(user);
            return AuthResponse.builder().token(token).user(toAuthUser(user)).build();
        }

        emailOtpService.verifyOtp(email, request.getOtp());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);
        return AuthResponse.builder().token(token).user(toAuthUser(savedUser)).build();
    }

    @Transactional
    public MessageResponse resendEmailOtp(ResendEmailOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthenticatedException("Tài khoản không tồn tại."));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new PermissionDeniedException("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AlreadyExistsException("Tài khoản đã được xác thực. Bạn có thể đăng nhập.");
        }

        emailOtpService.issueOtp(email, user.getFullName());
        return MessageResponse
            .builder()
            .message("OTP mới đã được gửi tới email của bạn.")
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthenticatedException("Email hoặc mật khẩu không chính xác."));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new PermissionDeniedException("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
        }

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new PermissionDeniedException("Tài khoản chưa xác thực email. Vui lòng nhập OTP để kích hoạt.");
        }

        try {
            authenticationManagerProvider.getObject().authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new UnauthenticatedException("Email hoặc mật khẩu không chính xác.");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder().token(token).user(toAuthUser(user)).build();
    }

    @Transactional
    public MessageResponse logout(String authorizationHeader, String cookieToken) {
        String token = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        } else if (cookieToken != null && !cookieToken.isBlank()) {
            token = cookieToken;
        }

        if (token == null || token.isBlank()) {
            throw new UnauthenticatedException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }

        try {
            tokenRevocationService.revoke(token, jwtService.extractExpiration(token));
        } catch (Exception e) {
            throw new UnauthenticatedException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }

        SecurityContextHolder.clearContext();
        return MessageResponse.builder().message("Đăng xuất thành công.").build();
    }

    @Transactional
    public AuthResponse authenticateGoogleUser(String email, String fullName, String avatarUrl) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new UnauthenticatedException("Không lấy được email từ Google.");
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            String effectiveFullName =
                (fullName == null || fullName.isBlank())
                    ? normalizedEmail.substring(0, normalizedEmail.indexOf("@"))
                    : fullName.trim();

            user = User
                .builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(effectiveFullName)
                .avatarUrl(avatarUrl)
                .authProvider("GOOGLE")
                .role("MEMBER")
                .status(UserStatus.ACTIVE)
                .build();
        } else {
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new PermissionDeniedException("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
            }

            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName.trim());
            }
            user.setAvatarUrl(avatarUrl);
            user.setAuthProvider("GOOGLE");

            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                user.setStatus(UserStatus.ACTIVE);
            }
        }

        User savedUser = userRepository.save(Objects.requireNonNull(user));
        String token = jwtService.generateToken(savedUser);
        return AuthResponse.builder().token(token).user(toAuthUser(savedUser)).build();
    }

    private AuthUserResponse toAuthUser(User user) {
        return AuthUserResponse
            .builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
