package com.rally.catalog.exception;

import com.rally.common.exceptions.handler.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class CatalogExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex,
                                                             HttpServletRequest req) {
        log.warn("Malformed request body at {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .title("Malformed Request")
                .message("Request body is not valid JSON")
                .path(req.getRequestURI())
                .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        log.warn("Invalid path/query value for '{}' at {}: {}", ex.getName(), req.getRequestURI(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .title("Invalid Request Parameter")
                .message("Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'")
                .path(req.getRequestURI())
                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest req) {
        log.error("Data integrity violation at {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .title("Data Integrity Violation")
                .message("The request conflicts with existing data")
                .path(req.getRequestURI())
                .build());
    }
}
