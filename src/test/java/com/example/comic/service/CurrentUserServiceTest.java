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
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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

    @Test
    void requireUser_shouldThrowWhenAuthenticationMissingNameOrNotAuthenticatedOrUserMissing() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(null, null)
        );
        SecurityContextHolder.getContext().getAuthentication().setAuthenticated(true);
        assertThrows(UnauthenticatedException.class, () -> currentUserService.requireUser());

        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("member@comic.local", "pwd")
        );
        assertThrows(UnauthenticatedException.class, () -> currentUserService.requireUser());

        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("member@comic.local", "pwd")
        );
        SecurityContextHolder.getContext().getAuthentication().setAuthenticated(true);
        when(userRepository.findByEmail("member@comic.local")).thenReturn(Optional.empty());
        assertThrows(UnauthenticatedException.class, () -> currentUserService.requireUser());
    }

    @Test
    void resolveRole_shouldReturnResolvedRoleOrGuestForInvalidAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("member@comic.local", "pwd", "ROLE_MEMBER")
        );
        when(userRepository.findByEmail("member@comic.local")).thenReturn(Optional.of(user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE)));
        assertEquals(UserRole.MEMBER, currentUserService.resolveRole());

        when(userRepository.findByEmail("member@comic.local")).thenReturn(Optional.empty());
        assertEquals(UserRole.GUEST, currentUserService.resolveRole());

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(null, null));
        SecurityContextHolder.getContext().getAuthentication().setAuthenticated(true);
        assertEquals(UserRole.GUEST, currentUserService.resolveRole());

        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        );
        assertEquals(UserRole.GUEST, currentUserService.resolveRole());
        verify(userRepository, never()).findByEmail("anonymousUser");
    }

    @Test
    void requireUser_shouldThrowWhenAuthenticationIsNullOrNameIsNull() {
        SecurityContextHolder.clearContext();
        assertThrows(UnauthenticatedException.class, () -> currentUserService.requireUser());

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(UnauthenticatedException.class, () -> currentUserService.requireUser());
    }

    @Test
    void resolveRole_shouldReturnGuestWhenNotAuthenticatedOrNameNull() {
        Authentication notAuthenticated = mock(Authentication.class);
        when(notAuthenticated.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(notAuthenticated);
        assertEquals(UserRole.GUEST, currentUserService.resolveRole());

        Authentication nullName = mock(Authentication.class);
        when(nullName.isAuthenticated()).thenReturn(true);
        when(nullName.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(nullName);
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
