package com.school.management.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 80)
    private String module;

    @Column(name = "record_id", length = 120)
    private String recordId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    protected AuditLog() {
    }

    public AuditLog(UUID actorUserId, String action, String module, String recordId,
                    Instant occurredAt, String ipAddress, String deviceInfo) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.module = module;
        this.recordId = recordId;
        this.occurredAt = occurredAt;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
    }
}
