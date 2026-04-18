package com.example.comic.security;

import com.example.comic.model.UserRole;
import com.example.comic.security.oauth2.OAuth2LoginFailureHandler;
import com.example.comic.security.oauth2.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(
                        "/auth/me",
                        "/auth/register",
                        "/auth/register-otp",
                        "/auth/login",
                        "/auth/verify-email-otp",
                        "/auth/resend-email-otp",
                        "/oauth2/**",
                        "/login/oauth2/**"
                    )
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/comics")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/chapters/*/pages")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/chapters/*/comments")
                    .permitAll()
                        .requestMatchers("/admin/**")
                    .hasRole(UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/comics")
                    .hasRole(UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/comics/*/chapters")
                    .hasRole(UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/chapters/*/pages")
                    .hasRole(UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/chapters/*/pages", "/chapters/pages/*")
                    .hasRole(UserRole.ADMIN.name())
                    .anyRequest()
                    .authenticated()
            )
            .authenticationProvider(authenticationProvider)
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler)
            )
            .oauth2Login(oauth2 ->
                oauth2
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
