package com.school.management.auth;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.school.management.audit.LoginAttemptRecorder;
import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {
    private static final String LOGIN_LOG_SESSION_KEY = "school.loginLogId";

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptRecorder loginAttemptRecorder;
    private final AppUserRepository users;
    private final SecurityContextRepository securityContextRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       LoginAttemptRecorder loginAttemptRecorder,
                       AppUserRepository users,
                       SecurityContextRepository securityContextRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptRecorder = loginAttemptRecorder;
        this.users = users;
        this.securityContextRepository = securityContextRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest,
                              HttpServletResponse servletResponse) {
        String username = request.username().trim();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    username, request.password()));
        } catch (AuthenticationException exception) {
            loginAttemptRecorder.recordFailure(username, clientAddress(servletRequest), userAgent(servletRequest));
            throw new BadCredentialsException("Invalid username or password.");
        }

        AppUser user = (AppUser) authentication.getPrincipal();
        var loginLog = loginAttemptRecorder.recordSuccess(user, clientAddress(servletRequest), userAgent(servletRequest));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(LOGIN_LOG_SESSION_KEY, loginLog.toString());

        return authResponse(user);
    }

    public AuthResponse refreshCurrentUser(AppUser principal, HttpServletRequest servletRequest,
                                           HttpServletResponse servletResponse) {
        AppUser user = users.findByUsernameIgnoreCase(principal.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));
        Authentication authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
            .authenticated(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return authResponse(user);
    }

    private AuthResponse authResponse(AppUser user) {
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

    public void changePassword(AppUser user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }
        if (request.newPassword().length() < 8
            || request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("New password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }
        loginAttemptRecorder.recordPasswordChange(user, passwordEncoder.encode(request.newPassword()));
    }

    public void logout(AppUser user, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object loginLogId = session.getAttribute(LOGIN_LOG_SESSION_KEY);
            if (loginLogId instanceof String id) {
                loginAttemptRecorder.recordLogout(java.util.UUID.fromString(id), user,
                    clientAddress(request), userAgent(request));
            } else if (user != null) {
                loginAttemptRecorder.recordLogout(null, user, clientAddress(request), userAgent(request));
            }
            session.invalidate();
        } else if (user != null) {
            loginAttemptRecorder.recordLogout(null, user, clientAddress(request), userAgent(request));
        }
        SecurityContextHolder.clearContext();
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        return value == null ? null : value.substring(0, Math.min(value.length(), 500));
    }
}
