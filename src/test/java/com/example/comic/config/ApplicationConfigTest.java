package com.example.comic.config;

import com.example.comic.model.User;
import com.example.comic.model.UserRole;
import com.example.comic.model.UserStatus;
import com.example.comic.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ApplicationConfigTest {

    private UserRepository userRepository;
    private ApplicationConfig applicationConfig;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        applicationConfig = new ApplicationConfig(userRepository);
    }

    @Test
    void userDetailsService_shouldLoadByEmail() {
        User user = User.builder().id(1L).email("user@example.com").passwordHash("hash").fullName("User").role(UserRole.MEMBER).status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetailsService service = applicationConfig.userDetailsService();

        assertEquals("user@example.com", service.loadUserByUsername("user@example.com").getUsername());
    }

    @Test
    void userDetailsService_shouldThrowWhenMissingUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UserDetailsService service = applicationConfig.userDetailsService();

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
    }

    @Test
    void authenticationProvider_andPasswordEncoder_shouldBeCreated() {
        AuthenticationProvider provider = applicationConfig.authenticationProvider(applicationConfig.userDetailsService());
        PasswordEncoder encoder = applicationConfig.passwordEncoder();

        assertNotNull(provider);
        assertNotNull(encoder);
        assertNotNull(encoder.encode("Password123"));
    }

    @Test
    void authenticationManager_shouldDelegateToAuthenticationConfiguration() throws Exception {
        AuthenticationConfiguration configuration = Mockito.mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = Mockito.mock(AuthenticationManager.class);
        when(configuration.getAuthenticationManager()).thenReturn(manager);

        AuthenticationManager result = applicationConfig.authenticationManager(configuration);

        assertEquals(manager, result);
    }
}
