package com.school.management.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_logs")
public class LoginLog {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username_attempt", nullable = false, length = 120)
    private String usernameAttempt;

    @Column(nullable = false)
    private boolean succeeded;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "logged_out_at")
    private Instant loggedOutAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    protected LoginLog() {
    }

    public LoginLog(UUID userId, String usernameAttempt, boolean succeeded, String failureReason,
                    Instant occurredAt, String ipAddress, String userAgent) {
        this.userId = userId;
        this.usernameAttempt = usernameAttempt;
        this.succeeded = succeeded;
        this.failureReason = failureReason;
        this.occurredAt = occurredAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setLoggedOutAt(Instant loggedOutAt) {
        this.loggedOutAt = loggedOutAt;
    }
}
