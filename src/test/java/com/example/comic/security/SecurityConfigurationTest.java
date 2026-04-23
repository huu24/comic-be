package com.example.comic.security;

import com.example.comic.security.oauth2.OAuth2LoginFailureHandler;
import com.example.comic.security.oauth2.OAuth2LoginSuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationTest {

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private AuthenticationProvider authenticationProvider;
    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private RestAccessDeniedHandler accessDeniedHandler;
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    private SecurityConfiguration securityConfiguration;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
        authenticationProvider = mock(AuthenticationProvider.class);
        authenticationEntryPoint = mock(RestAuthenticationEntryPoint.class);
        accessDeniedHandler = mock(RestAccessDeniedHandler.class);
        oAuth2LoginSuccessHandler = mock(OAuth2LoginSuccessHandler.class);
        oAuth2LoginFailureHandler = mock(OAuth2LoginFailureHandler.class);

        securityConfiguration = new SecurityConfiguration(
            jwtAuthenticationFilter,
            authenticationProvider,
            authenticationEntryPoint,
            accessDeniedHandler,
            oAuth2LoginSuccessHandler,
            oAuth2LoginFailureHandler
        );
    }

    @Test
    void securityFilterChain_shouldBuildAndReturnChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);

        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain actualChain = securityConfiguration.securityFilterChain(http);

        assertSame(expectedChain, actualChain);

        verify(http).csrf(any());
        verify(http).sessionManagement(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).authenticationProvider(authenticationProvider);
        verify(http).exceptionHandling(any());
        verify(http).oauth2Login(any());
        verify(http).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        verify(http).build();
    }

    @Test
    void securityFilterChain_shouldPropagateException() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        RuntimeException boom = new RuntimeException("build failed");

        when(http.build()).thenThrow(boom);

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> securityConfiguration.securityFilterChain(http)
        );

        assertSame(boom, thrown);
    }
}
