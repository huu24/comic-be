package com.example.comic.controller;

import com.example.comic.model.UserRole;
import com.example.comic.model.dto.AuthResponse;
import com.example.comic.model.dto.AuthUserResponse;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.security.AuthCookieService;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.AuthService;
import com.example.comic.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthCookieService authCookieService;

    @MockBean
    private CurrentUserService currentUserService;

    @Test
    void me_shouldReturnResolvedRole() throws Exception {
        when(currentUserService.resolveRole()).thenReturn(UserRole.ADMIN);

        mockMvc
            .perform(get("/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void register_shouldReturnCreated() throws Exception {
        AuthResponse response = AuthResponse
            .builder()
            .token("jwt-token")
            .user(AuthUserResponse.builder().id(1L).email("user@example.com").fullName("User").role("MEMBER").build())
            .build();
        when(authService.register(any())).thenReturn(response);

        mockMvc
            .perform(
                post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of("email", "user@example.com", "password", "Password123", "fullName", "User")
                        )
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    void register_shouldReturnBadRequestWhenInvalidPayload() throws Exception {
        mockMvc
            .perform(
                post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("email", "bad", "password", "123")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void registerWithOtp_shouldReturnCreatedMessage() throws Exception {
        when(authService.registerWithOtp(any()))
            .thenReturn(MessageResponse.builder().message("OTP sent").build());

        mockMvc
            .perform(
                post("/auth/register-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of("email", "user@example.com", "password", "Password123", "fullName", "User")
                        )
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("OTP sent"));
    }

    @Test
    void verifyEmailOtp_andResend_andLogin_shouldReturnOk() throws Exception {
        AuthResponse authResponse = AuthResponse
            .builder()
            .token("jwt-token")
            .user(AuthUserResponse.builder().id(1L).email("user@example.com").fullName("User").role("MEMBER").build())
            .build();
        when(authService.verifyEmailOtp(any())).thenReturn(authResponse);
        when(authService.resendEmailOtp(any())).thenReturn(MessageResponse.builder().message("resent").build());
        when(authService.login(any())).thenReturn(authResponse);

        mockMvc
            .perform(
                post("/auth/verify-email-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@example.com", "otp", "123456")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"));

        mockMvc
            .perform(
                post("/auth/resend-email-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@example.com")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("resent"));

        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@example.com", "password", "Password123")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void logout_shouldClearCookieAndReturnMessage() throws Exception {
        when(authCookieService.resolveToken(any())).thenReturn("cookie-token");
        when(authService.logout(eq("Bearer abc"), eq("cookie-token")))
            .thenReturn(MessageResponse.builder().message("Đăng xuất thành công.").build());
        when(authCookieService.buildClearCookie()).thenReturn("COMIC_AUTH=; Max-Age=0; Path=/; HttpOnly");

        mockMvc
            .perform(post("/auth/logout").header("Authorization", "Bearer abc"))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie", containsString("COMIC_AUTH=")))
            .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(jsonPath("$.message").value("Đăng xuất thành công."));

        verify(authService).logout("Bearer abc", "cookie-token");
    }
}
