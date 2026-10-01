package com.school.management.academics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_sessions",
    indexes = {
        @Index(name = "ix_attendance_sessions_date_class", columnList = "attendance_date, school_class_id", unique = true),
        @Index(name = "ix_attendance_sessions_academic_year", columnList = "academic_year_id")
    })
public class AttendanceSession {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "session_name", nullable = false, length = 80)
    private String sessionName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceSession() {
    }

    public AttendanceSession(AcademicYear academicYear, SchoolClass schoolClass, LocalDate attendanceDate, String sessionName) {
        this.academicYear = academicYear;
        this.schoolClass = schoolClass;
        this.attendanceDate = attendanceDate;
        this.sessionName = sessionName == null ? "Morning" : sessionName.trim();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public String getSessionName() {
        return sessionName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
