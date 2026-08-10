package com.rally.catalog.config;

// ============================================================================
// ADMIN role filter for /products/admin/** routes.
//
// DISABLED — NOT registered until the Auth service is implemented.
//
// Design: the catalog service does its OWN role filtering (no API Gateway
// dependency). It reads the X-User-Role header that the gateway injects after
// JWT validation. Until auth exists, that header is unverifiable, so this
// filter is switched off to keep the endpoints testable.
//
// To enable (when Auth service is ready):
//   1. Uncomment the @Component annotation below, OR
//   2. Uncomment the adminRoleFilter() bean in SecurityConfig.
// ============================================================================

import com.rally.common.exceptions.shared.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// @Component
public class AdminRoleFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/products/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String role = request.getHeader("X-User-Role");
        if (!"ADMIN".equals(role)) {
            throw new UnauthorizedException("ADMIN role required");
        }
        filterChain.doFilter(request, response);
    }
}
