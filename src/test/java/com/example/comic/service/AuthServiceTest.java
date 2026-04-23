package com.example.comic.service;

import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.User;
import com.example.comic.model.UserRole;
import com.example.comic.model.UserStatus;
import com.example.comic.model.dto.AuthResponse;
import com.example.comic.model.dto.LoginRequest;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.RegisterRequest;
import com.example.comic.model.dto.ResendEmailOtpRequest;
import com.example.comic.model.dto.VerifyEmailOtpRequest;
import com.example.comic.repository.UserRepository;
import com.example.comic.security.JwtService;
import com.example.comic.security.token.TokenRevocationService;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectProvider<AuthenticationManager> authenticationManagerProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private EmailOtpService emailOtpService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            passwordEncoder,
            jwtService,
            authenticationManagerProvider,
            tokenRevocationService,
            emailOtpService
        );
        when(authenticationManagerProvider.getObject()).thenReturn(authenticationManager);
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(
            RegisterRequest.builder().email(" TEST@EXAMPLE.COM ").password("Password123").fullName("  Test User  ").build()
        );

        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getFullName());
        assertEquals("MEMBER", response.getUser().getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserRole.MEMBER, userCaptor.getValue().getRole());
        assertEquals(UserStatus.ACTIVE, userCaptor.getValue().getStatus());
    }

    @Test
    void register_shouldRejectExistingEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(
            AlreadyExistsException.class,
            () -> authService.register(RegisterRequest.builder().email("test@example.com").password("Password123").fullName("Test").build())
        );
    }

    @Test
    void registerWithOtp_shouldCreatePendingUserAndIssueOtp() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.registerWithOtp(
            RegisterRequest.builder().email("test@example.com").password("Password123").fullName("  Test User  ").build()
        );

        assertEquals("Đăng ký thành công. Vui lòng nhập OTP 6 số đã gửi tới email để kích hoạt tài khoản.", response.getMessage());
        verify(emailOtpService).issueOtp("test@example.com", "Test User");
    }

    @Test
    void registerWithOtp_shouldRejectActiveAccount() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user(1L, "test@example.com", UserRole.MEMBER, UserStatus.ACTIVE)));

        assertThrows(
            AlreadyExistsException.class,
            () -> authService.registerWithOtp(RegisterRequest.builder().email("test@example.com").password("Password123").fullName("Test").build())
        );
    }

    @Test
    void registerWithOtp_shouldRejectLockedAccount() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user(1L, "test@example.com", UserRole.MEMBER, UserStatus.LOCKED)));

        assertThrows(
            PermissionDeniedException.class,
            () -> authService.registerWithOtp(RegisterRequest.builder().email("test@example.com").password("Password123").fullName("Test").build())
        );
    }

    @Test
    void registerWithOtp_shouldUpdatePendingAccountAndReissueOtp() {
        User existing = user(1L, "test@example.com", UserRole.MEMBER, UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.registerWithOtp(
            RegisterRequest.builder().email("test@example.com").password("Password123").fullName("  New Name  ").build()
        );

        assertEquals("Email đã đăng ký nhưng chưa xác thực. Hệ thống đã gửi lại OTP 6 số tới email của bạn.", response.getMessage());
        assertEquals("New Name", existing.getFullName());
        assertEquals(UserStatus.PENDING_VERIFICATION, existing.getStatus());
        verify(emailOtpService).issueOtp("test@example.com", "New Name");
    }

    @Test
    void verifyEmailOtp_shouldActivatePendingAccount() {
        User user = user(1L, "test@example.com", UserRole.MEMBER, UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        doNothing().when(emailOtpService).verifyOtp("test@example.com", "123456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.verifyEmailOtp(
            VerifyEmailOtpRequest.builder().email("TEST@EXAMPLE.COM").otp("123456").build()
        );

        assertEquals("jwt-token", response.getToken());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void verifyEmailOtp_shouldReturnTokenWhenAlreadyActive() {
        User user = user(1L, "test@example.com", UserRole.MEMBER, UserStatus.ACTIVE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.verifyEmailOtp(
            VerifyEmailOtpRequest.builder().email("test@example.com").otp("ignored").build()
        );

        assertEquals("jwt-token", response.getToken());
        verify(emailOtpService, org.mockito.Mockito.never()).verifyOtp(any(), any());
        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
    }

    @Test
    void verifyEmailOtp_shouldRejectLockedAccount() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user(1L, "test@example.com", UserRole.MEMBER, UserStatus.LOCKED)));

        assertThrows(
            PermissionDeniedException.class,
            () -> authService.verifyEmailOtp(VerifyEmailOtpRequest.builder().email("test@example.com").otp("123456").build())
        );
    }

    @Test
    void verifyEmailOtp_shouldRejectMissingAccount() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(
            UnauthenticatedException.class,
            () -> authService.verifyEmailOtp(VerifyEmailOtpRequest.builder().email("test@example.com").otp("123456").build())
        );
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        User user = user(1L, "test@example.com", UserRole.MEMBER, UserStatus.ACTIVE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
            new UsernamePasswordAuthenticationToken("test@example.com", "Password123")
        );
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(LoginRequest.builder().email("TEST@EXAMPLE.COM").password("Password123").build());

        assertEquals("jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldRejectInvalidPassword() {
        User user = user(1L, "test@example.com", UserRole.MEMBER, UserStatus.ACTIVE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("bad"));

        assertThrows(
            UnauthenticatedException.class,
            () -> authService.login(LoginRequest.builder().email("test@example.com").password("wrong").build())
        );
    }

    @Test
    void login_shouldRejectPendingAndLockedUsersBeforeAuthentication() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user(1L, "test@example.com", UserRole.MEMBER, UserStatus.PENDING_VERIFICATION)));
        assertThrows(
            PermissionDeniedException.class,
            () -> authService.login(LoginRequest.builder().email("test@example.com").password("pwd").build())
        );

        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(user(2L, "locked@example.com", UserRole.MEMBER, UserStatus.LOCKED)));
        assertThrows(
            PermissionDeniedException.class,
            () -> authService.login(LoginRequest.builder().email("locked@example.com").password("pwd").build())
        );
    }

    @Test
    void logout_shouldRevokeTokenAndClearContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("test@example.com", "Password123")
        );
        when(jwtService.extractExpiration("abc.def.ghi")).thenReturn(Date.from(Instant.now().plusSeconds(60)));

        MessageResponse response = authService.logout("Bearer abc.def.ghi", null);

        assertEquals("Đăng xuất thành công.", response.getMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenRevocationService).revoke(eq("abc.def.ghi"), any(Date.class));
    }

    @Test
    void logout_shouldUseCookieTokenAndRejectInvalidRequests() {
        when(jwtService.extractExpiration("cookie-token")).thenReturn(Date.from(Instant.now().plusSeconds(60)));

        MessageResponse response = authService.logout(null, "cookie-token");

        assertEquals("Đăng xuất thành công.", response.getMessage());
        verify(tokenRevocationService).revoke(eq("cookie-token"), any(Date.class));

        assertThrows(UnauthenticatedException.class, () -> authService.logout(null, "   "));
        assertThrows(UnauthenticatedException.class, () -> authService.logout("Bearer ", null));
    }

    @Test
    void logout_shouldRejectWhenRevokeFails() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("test@example.com", "Password123")
        );
        when(jwtService.extractExpiration("abc.def.ghi")).thenThrow(new RuntimeException("bad token"));

        assertThrows(UnauthenticatedException.class, () -> authService.logout("Bearer abc.def.ghi", null));
    }

    @Test
    void authenticateGoogleUser_shouldCreateNewGoogleAccount() {
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.authenticateGoogleUser(" google@example.com ", "  ", "avatar-url");

        assertEquals("jwt-token", response.getToken());
        assertEquals("google", response.getUser().getFullName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticateGoogleUser_shouldUpdateExistingPendingAccount() {
        User existing = user(9L, "google@example.com", UserRole.MEMBER, UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        AuthResponse response = authService.authenticateGoogleUser(" google@example.com ", " New Name ", "avatar-updated");

        assertEquals("jwt-token", response.getToken());
        assertEquals("New Name", existing.getFullName());
        assertEquals(UserStatus.ACTIVE, existing.getStatus());
        assertEquals("GOOGLE", existing.getAuthProvider());
    }

    @Test
    void authenticateGoogleUser_shouldRejectBlankEmailAndLockedAccount() {
        assertThrows(UnauthenticatedException.class, () -> authService.authenticateGoogleUser("   ", "Name", "avatar"));

        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(user(1L, "locked@example.com", UserRole.MEMBER, UserStatus.LOCKED)));
        assertThrows(
            PermissionDeniedException.class,
            () -> authService.authenticateGoogleUser("locked@example.com", "Name", "avatar")
        );
    }

    private static User user(Long id, String email, UserRole role, UserStatus status) {
        return User
            .builder()
            .id(id)
            .email(email)
            .passwordHash("hash")
            .fullName("Test User")
            .role(role)
            .status(status)
            .build();
    }
}
