package com.parkease.analytics.web.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .status(403).error("Forbidden")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                .status(404).error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse.builder()
                .status(502).error("Bad Gateway")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
        NoResourceFoundException ex,
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
                .status(500).error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
