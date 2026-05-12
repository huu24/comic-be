package com.example.comic.security;

import com.example.comic.security.token.TokenRevocationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private TokenRevocationService tokenRevocationService;
    private AuthCookieService authCookieService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = Mockito.mock(JwtService.class);
        userDetailsService = Mockito.mock(UserDetailsService.class);
        tokenRevocationService = Mockito.mock(TokenRevocationService.class);
        authCookieService = Mockito.mock(AuthCookieService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, tokenRevocationService, authCookieService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldContinueWhenNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);
        when(authCookieService.resolveToken(request)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldSetAuthenticationForValidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("user@example.com").password("pwd").authorities("ROLE_MEMBER").build();
        when(tokenRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSkipWhenTokenRevokedOrInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        when(tokenRevocationService.isRevoked("revoked-token")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(jwtService, never()).extractUsername(any());
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldIgnoreJwtParsingException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        when(tokenRevocationService.isRevoked("broken-token")).thenReturn(false);
        when(jwtService.extractUsername("broken-token")).thenThrow(new RuntimeException("bad token"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldUseCookieTokenWhenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("cookie@example.com").password("pwd").authorities("ROLE_MEMBER").build();
        when(authCookieService.resolveToken(request)).thenReturn("cookie-token");
        when(tokenRevocationService.isRevoked("cookie-token")).thenReturn(false);
        when(jwtService.extractUsername("cookie-token")).thenReturn("cookie@example.com");
        when(userDetailsService.loadUserByUsername("cookie@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("cookie-token", userDetails)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("cookie@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldFallbackToCookieWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("cookie2@example.com").password("pwd").authorities("ROLE_MEMBER").build();
        when(authCookieService.resolveToken(request)).thenReturn("cookie-token-2");
        when(tokenRevocationService.isRevoked("cookie-token-2")).thenReturn(false);
        when(jwtService.extractUsername("cookie-token-2")).thenReturn("cookie2@example.com");
        when(userDetailsService.loadUserByUsername("cookie2@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("cookie-token-2", userDetails)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("cookie2@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueWhenResolvedTokenIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        when(authCookieService.resolveToken(request)).thenReturn("   ");

        filter.doFilter(request, response, chain);

        verify(tokenRevocationService, never()).isRevoked(any());
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldContinueWhenExtractedEmailIsNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-without-email");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        when(tokenRevocationService.isRevoked("token-without-email")).thenReturn(false);
        when(jwtService.extractUsername("token-without-email")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateWhenTokenValidationFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("user@example.com").password("pwd").authorities("ROLE_MEMBER").build();
        when(tokenRevocationService.isRevoked("invalid-token")).thenReturn(false);
        when(jwtService.extractUsername("invalid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-token", userDetails)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotOverrideExistingAuthentication() throws Exception {
        SecurityContextHolder
            .getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("existing", null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        when(tokenRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");

        filter.doFilter(request, response, chain);

        verify(userDetailsService, never()).loadUserByUsername(any());
        assertEquals("existing", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }
}
