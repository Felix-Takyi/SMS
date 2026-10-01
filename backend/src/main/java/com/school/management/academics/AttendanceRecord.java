package com.school.management.academics;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.school.management.students.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_records",
    indexes = {
        @Index(name = "ix_attendance_records_session_student", columnList = "attendance_session_id, student_id", unique = true),
        @Index(name = "ix_attendance_records_status", columnList = "status")
    })
public class AttendanceRecord {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession attendanceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(AttendanceSession attendanceSession, Student student, String status, String note) {
        this.attendanceSession = attendanceSession;
        this.student = student;
        this.status = status == null || status.isBlank() ? "PRESENT" : status.trim();
        this.note = note == null ? "" : note.trim();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public AttendanceSession getAttendanceSession() {
        return attendanceSession;
    }

    public Student getStudent() {
        return student;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
