package com.example.comic.security;

import com.example.comic.model.UserRole;
import com.example.comic.security.oauth2.OAuth2LoginFailureHandler;
import com.example.comic.security.oauth2.OAuth2LoginSuccessHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

        private static final String ADMIN_ROLE = UserRole.ADMIN.name();

        private static final String ALL_PATHS = "/**";

        private final CorsProperties corsProperties;

        private static final String[] PUBLIC_ENDPOINTS = {
                        "/auth/me",
                        "/auth/register",
                        "/auth/register-otp",
                        "/auth/login",
                        "/auth/verify-email-otp",
                        "/auth/resend-email-otp",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/oauth2/**",
                        "/login/oauth2/**"
        };

        private static final String[] PUBLIC_GET_ENDPOINTS = {
                        "/comics",
                        "/comics/*",
                        "/comics/*/chapters",
                        "/chapters/*/pages",
                        "comics/*/book-overview",
                        "/chapters/*/comments",
                        "/categories"
        };

        private static final String[] ADMIN_POST_ENDPOINTS = {
                        "/comics",
                        "/comics/*/chapters",
                        "/chapters/*/pages",
                        "/categories"
        };

        private static final String[] ADMIN_PUT_ENDPOINTS = {
                        "/categories/*"
        };

        private static final String[] ADMIN_DELETE_ENDPOINTS = {
                        "/chapters/*/pages",
                        "/chapters/pages/*",
                        "/categories/*"
        };

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AuthenticationProvider authenticationProvider;
        private final RestAuthenticationEntryPoint authenticationEntryPoint;
        private final RestAccessDeniedHandler accessDeniedHandler;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> {
                                        permitAll(auth, HttpMethod.OPTIONS, ALL_PATHS);
                                        permitAll(auth, PUBLIC_ENDPOINTS);
                                        permitAll(auth, HttpMethod.GET, PUBLIC_GET_ENDPOINTS);

                                        hasRole(auth, ADMIN_ROLE, "/admin/**");
                                        hasRole(auth, HttpMethod.POST, ADMIN_ROLE, ADMIN_POST_ENDPOINTS);
                                        hasRole(auth, HttpMethod.PUT, ADMIN_ROLE, ADMIN_PUT_ENDPOINTS);
                                        hasRole(auth, HttpMethod.DELETE, ADMIN_ROLE, ADMIN_DELETE_ENDPOINTS);

                                        auth.anyRequest().authenticated();
                                })
                                .authenticationProvider(authenticationProvider)
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oAuth2LoginSuccessHandler)
                                                .failureHandler(oAuth2LoginFailureHandler))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                List<String> origins = corsProperties.getAllowedOrigins() == null
                        ? List.of()
                        : corsProperties.getAllowedOrigins()
                                .stream()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();

                if (origins.isEmpty()) {
                        throw new IllegalStateException(
                                "CORS cấu hình không hợp lệ: application.cors.allowed-origins đang rỗng. " +
                                        "Hãy set CORS_ALLOWED_ORIGINS với danh sách origin cụ thể.");
                }

                if (origins.stream().anyMatch("*"::equals)) {
                        throw new IllegalStateException(
                                "CORS cấu hình không hợp lệ: không được dùng '*' khi allowCredentials=true. " +
                                        "Hãy set CORS_ALLOWED_ORIGINS với origin cụ thể.");
                }

                config.setAllowedOriginPatterns(origins);

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization", "Set-Cookie", "Location"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration(ALL_PATHS, config);
                return source;
        }

        private static void permitAll(
                        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                        String... patterns) {
                auth.requestMatchers(patterns).permitAll();
        }

        private static void permitAll(
                        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                        HttpMethod method,
                        String... patterns) {
                auth.requestMatchers(method, patterns).permitAll();
        }

        private static void hasRole(
                        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                        String role,
                        String... patterns) {
                auth.requestMatchers(patterns).hasRole(role);
        }

        private static void hasRole(
                        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                        HttpMethod method,
                        String role,
                        String... patterns) {
                auth.requestMatchers(method, patterns).hasRole(role);
        }
}
