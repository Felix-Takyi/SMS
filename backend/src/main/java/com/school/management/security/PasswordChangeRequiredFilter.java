package com.school.management.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_PATHS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/me",
        "/api/v1/auth/change-password",
        "/api/v1/auth/logout",
        "/api/v1/auth/csrf"
    );

    private final AppUserRepository users;

    public PasswordChangeRequiredFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof AppUser principal) {
            AppUser user = users.findByUsernameIgnoreCase(principal.getUsername()).orElse(null);
            if (user != null) {
                var refreshed = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                    .authenticated(user, null, user.getAuthorities());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(refreshed);
                SecurityContextHolder.setContext(context);
                if (user.isPasswordChangeRequired() && !ALLOWED_PATHS.contains(request.getServletPath())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Change your temporary password to continue.\"}");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}