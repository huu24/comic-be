package com.example.comic.security;

import com.example.comic.model.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse
            .builder()
            .error(
                ErrorResponse.ErrorDetail
                    .builder()
                    .code(HttpStatus.FORBIDDEN.value())
                    .status("PERMISSION_DENIED")
                    .message("Bạn không có quyền truy cập tài nguyên này.")
                    .build()
            )
            .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
