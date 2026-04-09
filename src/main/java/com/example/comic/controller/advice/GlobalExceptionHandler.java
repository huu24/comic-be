package com.example.comic.controller.advice;

import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.NotFoundException;
import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.dto.ErrorResponse;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", message);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(AlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler({ IllegalArgumentException.class, DateTimeParseException.class })
    public ResponseEntity<ErrorResponse> handleInvalidArgument(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "Đã có lỗi hệ thống xảy ra.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorStatus, String message) {
        return ResponseEntity
            .status(status)
            .body(
                ErrorResponse
                    .builder()
                    .error(
                        ErrorResponse.ErrorDetail
                            .builder()
                            .code(status.value())
                            .status(errorStatus)
                            .message(message)
                            .build()
                    )
                    .build()
            );
    }
}
