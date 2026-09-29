package com.school.management.auth;

import com.school.management.audit.AuditLog;
import com.school.management.audit.AuditLogRepository;
import com.school.management.audit.LoginLog;
import com.school.management.audit.LoginLogRepository;
import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String LOGIN_LOG_SESSION_KEY = "school.loginLogId";

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final LoginLogRepository loginLogs;
    private final AuditLogRepository auditLogs;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(AuthenticationManager authenticationManager, AppUserRepository users,
                       LoginLogRepository loginLogs, AuditLogRepository auditLogs,
                       SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.loginLogs = loginLogs;
        this.auditLogs = auditLogs;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest,
                              HttpServletResponse servletResponse) {
        String username = request.username().trim();
        Instant now = Instant.now();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    username, request.password()));
        } catch (AuthenticationException exception) {
            recordFailure(username, servletRequest, now, exception);
            throw new BadCredentialsException("Invalid username or password.");
        }

        AppUser user = (AppUser) authentication.getPrincipal();
        user.registerSuccessfulLogin(now);
        users.save(user);

        LoginLog loginLog = loginLogs.saveAndFlush(new LoginLog(
            user.getId(), username, true, null, now,
            clientAddress(servletRequest), userAgent(servletRequest)));
        auditLogs.save(new AuditLog(user.getId(), "LOGIN", "AUTH", user.getId().toString(), now,
            clientAddress(servletRequest), userAgent(servletRequest)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(LOGIN_LOG_SESSION_KEY, loginLog.getId().toString());

        var permissions = user.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .sorted()
            .toList();
        var roles = user.getRoles().stream()
            .map(role -> role.getCode())
            .sorted(Comparator.naturalOrder())
            .toList();
        return new AuthResponse(user.getId(), user.getUsername(), user.getDisplayName(), roles, permissions);
    }

    @Transactional
    public void logout(AppUser user, HttpServletRequest request) {
        Instant now = Instant.now();
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object loginLogId = session.getAttribute(LOGIN_LOG_SESSION_KEY);
            if (loginLogId instanceof String id) {
                loginLogs.findById(java.util.UUID.fromString(id)).ifPresent(log -> {
                    log.setLoggedOutAt(now);
                    loginLogs.save(log);
                });
            }
            session.invalidate();
        }
        if (user != null) {
            auditLogs.save(new AuditLog(user.getId(), "LOGOUT", "AUTH", user.getId().toString(), now,
                clientAddress(request), userAgent(request)));
        }
        SecurityContextHolder.clearContext();
    }

    private void recordFailure(String username, HttpServletRequest request, Instant now,
                               AuthenticationException exception) {
        var user = users.findByUsernameIgnoreCase(username).orElse(null);
        String reason = exception instanceof AuthenticationServiceException
            ? "AUTHENTICATION_SERVICE_ERROR" : "INVALID_CREDENTIALS";
        if (user != null && !user.isAccountNonLocked()) {
            reason = "ACCOUNT_LOCKED";
        }
        if (user != null && user.isEnabled()) {
            user.registerFailedLogin(now, MAX_FAILED_ATTEMPTS, now.plus(LOCK_DURATION));
            users.save(user);
        }
        loginLogs.save(new LoginLog(user == null ? null : user.getId(), username, false, reason,
            now, clientAddress(request), userAgent(request)));
        auditLogs.save(new AuditLog(user == null ? null : user.getId(), "LOGIN_FAILED", "AUTH",
            username, now, clientAddress(request), userAgent(request)));
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        return value == null ? null : value.substring(0, Math.min(value.length(), 500));
    }
}
