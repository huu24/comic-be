package com.example.comic.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2LoginFailureHandlerTest {

    private OAuth2LoginFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2LoginFailureHandler();
        ReflectionTestUtils.setField(failureHandler, "failureRedirectUrl", "http://localhost:3000/oauth2/failure");
    }

    @Test
    void onAuthenticationFailure_shouldRedirectWithEncodedError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Email or password invalid"));

        assertTrue(response.getRedirectedUrl().startsWith("http://localhost:3000/oauth2/failure?error="));
        assertTrue(response.getRedirectedUrl().contains("Email+or+password+invalid"));
    }
}
