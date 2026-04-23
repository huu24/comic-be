package com.example.comic.controller.advice;

import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.NotFoundException;
import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.dto.ErrorResponse;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleAlreadyExists_shouldReturnConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleAlreadyExists(new AlreadyExistsException("Email đã tồn tại."));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("ALREADY_EXISTS", response.getBody().getError().getStatus());
        assertEquals("Email đã tồn tại.", response.getBody().getError().getMessage());
    }

    @Test
    void handleAuthErrors_shouldReturnExpectedCodes() {
        ResponseEntity<ErrorResponse> unauthenticated = handler.handleUnauthenticated(new UnauthenticatedException("Need login"));
        ResponseEntity<ErrorResponse> forbidden = handler.handlePermissionDenied(new PermissionDeniedException("Denied"));
        ResponseEntity<ErrorResponse> notFound = handler.handleNotFound(new NotFoundException("Not found"));

        assertEquals(401, unauthenticated.getStatusCode().value());
        assertEquals("UNAUTHENTICATED", unauthenticated.getBody().getError().getStatus());

        assertEquals(403, forbidden.getStatusCode().value());
        assertEquals("PERMISSION_DENIED", forbidden.getBody().getError().getStatus());

        assertEquals(404, notFound.getStatusCode().value());
        assertEquals("NOT_FOUND", notFound.getBody().getError().getStatus());
    }

    @Test
    void handleInvalidArgument_shouldReturnBadRequest() {
        ResponseEntity<ErrorResponse> fromIllegal = handler.handleInvalidArgument(new IllegalArgumentException("Sai dữ liệu"));
        ResponseEntity<ErrorResponse> fromDateParse = handler.handleInvalidArgument(
            new DateTimeParseException("invalid", "oops", 0)
        );

        assertEquals(400, fromIllegal.getStatusCode().value());
        assertEquals("INVALID_ARGUMENT", fromIllegal.getBody().getError().getStatus());
        assertEquals("Sai dữ liệu", fromIllegal.getBody().getError().getMessage());

        assertEquals(400, fromDateParse.getStatusCode().value());
        assertEquals("INVALID_ARGUMENT", fromDateParse.getBody().getError().getStatus());
    }

    @Test
    void handleTypeMismatch_shouldIncludeParameterName() {
        MethodArgumentTypeMismatchException mismatch = mock(MethodArgumentTypeMismatchException.class);
        when(mismatch.getName()).thenReturn("page");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(mismatch);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().getError().getMessage().contains("page"));
    }

    @Test
    void handleUnsupportedAndOther_shouldReturnExpectedStatuses() {
        ResponseEntity<ErrorResponse> unsupported = handler.handleUnsupportedMediaType(
            new HttpMediaTypeNotSupportedException("text/plain")
        );
        ResponseEntity<ErrorResponse> other = handler.handleOther(new RuntimeException("boom"));

        assertEquals(415, unsupported.getStatusCode().value());
        assertEquals("INVALID_ARGUMENT", unsupported.getBody().getError().getStatus());

        assertEquals(500, other.getStatusCode().value());
        assertEquals("INTERNAL", other.getBody().getError().getStatus());
        assertEquals("Đã có lỗi hệ thống xảy ra.", other.getBody().getError().getMessage());
    }
}
