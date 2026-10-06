package com.school.management.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.audit.AuditLog;
import com.school.management.audit.AuditLogRepository;
import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;
import com.school.management.users.RoleRepository;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogs;
    private final String username;
    private final String password;

    public InitialAdminBootstrap(AppUserRepository users, RoleRepository roles,
                                 PasswordEncoder passwordEncoder, AuditLogRepository auditLogs,
                                 @Value("${school.bootstrap.username:}") String username,
                                 @Value("${school.bootstrap.password:}") String password) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.auditLogs = auditLogs;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                "No users exist. Set BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD for first startup.");
        }
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
            "Bootstrap password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }
        var superAdminRole = roles.findByCode("SUPER_ADMIN")
            .orElseThrow(() -> new IllegalStateException("The SUPER_ADMIN role is missing from the database."));
        AppUser admin = new AppUser(username.trim(), "System Administrator", null,
            passwordEncoder.encode(password));
        admin.getRoles().add(superAdminRole);
        AppUser saved = users.saveAndFlush(admin);
        auditLogs.save(new AuditLog(saved.getId(), "INITIAL_ADMIN_CREATED", "USERS",
            saved.getId().toString(), Instant.now(), null, "Bootstrap"));
    }
}
