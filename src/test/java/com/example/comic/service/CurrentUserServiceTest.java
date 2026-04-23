package com.example.comic.service;

import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.User;
import com.example.comic.model.UserRole;
import com.example.comic.model.UserStatus;
import com.example.comic.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireUser_shouldReturnLoggedInUser() {
        User user = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByEmail("admin@comic.local")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin@comic.local", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        User actual = currentUserService.requireUser();

        assertSame(user, actual);
    }

    @Test
    void requireUser_shouldThrowWhenAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        );

        assertThrows(UnauthenticatedException.class, () -> currentUserService.requireUser());
    }

    @Test
    void requireAdmin_shouldReturnAdminUser() {
        User user = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByEmail("admin@comic.local")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin@comic.local", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        User actual = currentUserService.requireAdmin();

        assertSame(user, actual);
    }

    @Test
    void requireAdmin_shouldThrowWhenNotAdmin() {
        User user = user(1L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        when(userRepository.findByEmail("member@comic.local")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("member@comic.local", "password", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))
        );

        assertThrows(PermissionDeniedException.class, () -> currentUserService.requireAdmin());
    }

    @Test
    void resolveRole_shouldReturnGuestWhenNotAuthenticated() {
        assertEquals(UserRole.GUEST, currentUserService.resolveRole());
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
