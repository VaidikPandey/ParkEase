package com.parkease.auth.web.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                (a, b) -> a   // keep first message if duplicate field
            ));

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
            .status(400)
            .error("Validation Failed")
            .message("One or more fields are invalid")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .fieldErrors(fieldErrors)
            .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
        BadCredentialsException ex,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.builder()
            .status(401)
            .error("Unauthorized")
            .message("Invalid email or password")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
        UsernameNotFoundException ex,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
            .status(404)
            .error("Not Found")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
            .status(403)
            .error("Forbidden")
            .message("You do not have permission to access this resource")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
            .status(400)
            .error("Bad Request")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
        IllegalStateException ex,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
            .status(409)
            .error("Conflict")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
            .status(500)
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build());
    }
}
