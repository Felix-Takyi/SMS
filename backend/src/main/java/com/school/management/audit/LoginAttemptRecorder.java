package com.school.management.audit;

import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class LoginAttemptRecorder {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AppUserRepository users;
    private final LoginLogRepository loginLogs;
    private final AuditLogRepository auditLogs;

    public LoginAttemptRecorder(AppUserRepository users, LoginLogRepository loginLogs,
                                AuditLogRepository auditLogs) {
        this.users = users;
        this.loginLogs = loginLogs;
        this.auditLogs = auditLogs;
    }

    @Transactional
    public UUID recordSuccess(AppUser user, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        user.registerSuccessfulLogin(now);
        users.save(user);
        LoginLog loginLog = loginLogs.saveAndFlush(new LoginLog(
            user.getId(), user.getUsername(), true, null, now, ipAddress, userAgent));
        auditLogs.save(new AuditLog(user.getId(), "LOGIN", "AUTH", user.getId().toString(),
            now, ipAddress, userAgent));
        return loginLog.getId();
    }

    @Transactional
    public void recordFailure(String username, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        AppUser user = users.findLockedByUsername(username).orElse(null);
        String reason = "INVALID_CREDENTIALS";
        if (user != null && user.isEnabled()) {
            if (!user.isAccountNonLocked()) {
                reason = "ACCOUNT_LOCKED";
            } else {
                user.registerFailedLogin(now, MAX_FAILED_ATTEMPTS, now.plus(LOCK_DURATION));
                users.save(user);
            }
        }
        loginLogs.save(new LoginLog(user == null ? null : user.getId(), username, false, reason,
            now, ipAddress, userAgent));
        auditLogs.save(new AuditLog(user == null ? null : user.getId(), "LOGIN_FAILED", "AUTH",
            username, now, ipAddress, userAgent));
    }

    @Transactional
    public void recordLogout(UUID loginLogId, AppUser user, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        if (loginLogId != null) {
            loginLogs.findById(loginLogId).ifPresent(log -> {
                log.setLoggedOutAt(now);
                loginLogs.save(log);
            });
        }
        auditLogs.save(new AuditLog(user.getId(), "LOGOUT", "AUTH", user.getId().toString(),
            now, ipAddress, userAgent));
    }

    @Transactional
    public void recordPasswordChange(AppUser user, String encodedPassword) {
        user.changePasswordHash(encodedPassword);
        users.save(user);
        auditLogs.save(new AuditLog(user.getId(), "PASSWORD_CHANGED", "AUTH", user.getId().toString(),
            Instant.now(), null, null));
    }
}
