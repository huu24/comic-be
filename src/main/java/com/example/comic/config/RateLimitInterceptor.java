package com.example.comic.config;

import com.example.comic.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor
        implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long>
            rateLimitScript;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RateLimit rateLimit =
                method.getMethodAnnotation(
                        RateLimit.class
                );

        if (rateLimit == null) {
            return true;
        }

        String redisKey =
                buildRateLimitKey(request);

        long ttlSeconds =
                rateLimit.unit()
                        .toSeconds(
                                rateLimit.duration()
                        );

        Long currentCount;
        try {
            currentCount =
                    redisTemplate.execute(
                            rateLimitScript,
                            Collections.singletonList(
                                    redisKey
                            ),
                            String.valueOf(ttlSeconds)
                    );
        } catch (RuntimeException ex) {
            log.warn(
                    "Rate limit Redis operation failed for key {}. Allowing request to continue.",
                    redisKey,
                    ex
            );
            return true;
        }

        if (currentCount != null
                && currentCount > rateLimit.limit()) {

            response.setStatus(429);
            response.setContentType(
                    "application/json"
            );

            response.getWriter().write("""
                {
                  "message":"Too many requests"
                }
                """);

            return false;
        }

        return true;
    }

    private String buildRateLimitKey(
            HttpServletRequest request
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String endpoint =
                request.getRequestURI();

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            String email =
                    authentication.getName();

            return "rate_limit:"
                    + endpoint
                    + ":user:"
                    + email;
        }

        String ip =
                request.getRemoteAddr();

        return "rate_limit:"
                + endpoint
                + ":ip:"
                + ip;
    }
}
