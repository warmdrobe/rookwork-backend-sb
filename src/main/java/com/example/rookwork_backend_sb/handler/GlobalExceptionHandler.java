package com.example.rookwork_backend_sb.handler;

import com.example.rookwork_backend_sb.dtos.ErrorResponse;
import com.example.rookwork_backend_sb.exceptions.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt tất cả AppException và các class con (404, 403, 401, 400, 409...)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex, HttpServletRequest request) {

        return buildResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    // Bắt lỗi @Valid / @Validated trên request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    // Bắt lỗi AccessDeniedException của Spring Security (403)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    // Bắt lỗi sai HTTP Method (ví dụ: gửi DELETE vào endpoint chỉ nhận POST) - 405
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request.getRequestURI());
    }

    // Bắt lỗi sai kiểu dữ liệu tham số (ví dụ: truyền String "abc" vào tham số UUID) - 400
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type '%s'", ex.getName(), ex.getRequiredType().getSimpleName());
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    // Bắt lỗi Optimistic Locking (concurrent reorder conflict) - 409
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            org.springframework.dao.OptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT,
                "The resource was modified by another user. Please refresh and try again.",
                request.getRequestURI());
    }

    // Bắt tất cả exception còn lại (fallback - 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ex.printStackTrace(); // Log to console to debug 500 error
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                request.getRequestURI()
        );
    }

    // Helper
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, String path) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}