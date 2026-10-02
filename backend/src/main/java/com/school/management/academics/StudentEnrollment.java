package com.school.management.academics;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "student_enrollments",
    indexes = {
        @Index(name = "ix_student_enrollments_student_year", columnList = "student_id, academic_year_id"),
        @Index(name = "ix_student_enrollments_status", columnList = "status"),
        @Index(name = "ix_student_enrollments_class", columnList = "school_class_id")
    })
public class StudentEnrollment {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_stream_id")
    private ClassStream classStream;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentEnrollment() {
    }

    public StudentEnrollment(Student student, AcademicYear academicYear, SchoolClass schoolClass,
                            ClassStream classStream, String status) {
        this.student = student;
        this.academicYear = academicYear;
        this.schoolClass = schoolClass;
        this.classStream = classStream;
        this.status = status == null || status.isBlank() ? "ACTIVE" : status.trim();
        this.enrollmentDate = LocalDate.now();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(Student student, AcademicYear academicYear, SchoolClass schoolClass,
                       ClassStream classStream, String status) {
        this.student = student;
        this.academicYear = academicYear;
        this.schoolClass = schoolClass;
        this.classStream = classStream;
        this.status = status.trim();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public ClassStream getClassStream() {
        return classStream;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
