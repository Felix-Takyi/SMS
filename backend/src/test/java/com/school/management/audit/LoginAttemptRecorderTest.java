package com.school.management.audit;

import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAttemptRecorderTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final LoginLogRepository loginLogs = mock(LoginLogRepository.class);
    private final AuditLogRepository auditLogs = mock(AuditLogRepository.class);
    private final LoginAttemptRecorder recorder = new LoginAttemptRecorder(users, loginLogs, auditLogs);

    @Test
    void recordsFailureForUnknownUsernameWithoutRevealingAnAccount() {
        when(users.findLockedByUsername("unknown")).thenReturn(Optional.empty());

        recorder.recordFailure("unknown", "127.0.0.1", "test-agent");

        verify(loginLogs).save(any(LoginLog.class));
        verify(auditLogs).save(any(AuditLog.class));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void appliesTemporaryLockAfterFifthFailedAttempt() {
        AppUser user = new AppUser("teacher", "Teacher", null,
            new BCryptPasswordEncoder().encode("a-long-test-password"));
        when(users.findLockedByUsername("teacher")).thenReturn(Optional.of(user));

        for (int attempt = 0; attempt < 5; attempt++) {
            recorder.recordFailure("teacher", "127.0.0.1", "test-agent");
        }

        assertEquals(0, user.getFailedLoginAttempts());
        org.junit.jupiter.api.Assertions.assertFalse(user.isAccountNonLocked());
        verify(users, org.mockito.Mockito.times(5)).save(user);
        verify(loginLogs, org.mockito.Mockito.times(5)).save(any(LoginLog.class));
        verify(auditLogs, org.mockito.Mockito.times(5)).save(any(AuditLog.class));
    }

    @Test
    void recordsLogoutTimeOnSuccessfulLoginEntry() {
        var loginId = java.util.UUID.randomUUID();
        LoginLog login = mock(LoginLog.class);
        when(loginLogs.findById(loginId)).thenReturn(Optional.of(login));
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(java.util.UUID.randomUUID());

        recorder.recordLogout(loginId, user, "127.0.0.1", "test-agent");

        verify(login).setLoggedOutAt(any(Instant.class));
        verify(loginLogs).save(login);
        verify(auditLogs).save(any(AuditLog.class));
    }
}
