package com.rally.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.catalog.entity.Role;
import com.rally.common.exceptions.handler.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

/**
 * Enforces ADMIN on /products/admin/** by reading the trusted X-User-Role header the
 * gateway injects after JWT validation. Defense in depth: the service does its own
 * authorization even if the gateway route is bypassed. Rejects with 403 in the standard
 * rally-common ErrorResponse shape (a filter cannot rely on the @RestControllerAdvice).
 */
@Component
public class AdminRoleFilter extends OncePerRequestFilter {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final ObjectMapper objectMapper;

    public AdminRoleFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/products/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String role = request.getHeader(USER_ROLE_HEADER);
        if (role == null || !isAdmin(role)) {
            writeForbidden(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAdmin(String role) {
        return Arrays.stream(role.split(","))
                .map(String::trim)
                .anyMatch(Role.ADMIN.name()::equals);
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .title("Unauthorized")
                .message("ADMIN role required")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
