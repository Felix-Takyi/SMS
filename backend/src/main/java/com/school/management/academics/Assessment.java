package com.school.management.academics;

import java.math.BigDecimal;
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
@Table(name = "assessments",
    indexes = {
        @Index(name = "ix_assessments_subject_class_year_type", columnList = "subject_id, academic_year_id, school_class_id, assessment_type", unique = true),
        @Index(name = "ix_assessments_date", columnList = "assessment_date")
    })
public class Assessment {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "assessment_type", nullable = false, length = 40)
    private String assessmentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Assessment() {
    }

    public Assessment(Subject subject, AcademicYear academicYear, SchoolClass schoolClass,
                      String assessmentType, BigDecimal totalMarks, LocalDate assessmentDate, LocalDate dueDate) {
        this.subject = subject;
        this.academicYear = academicYear;
        this.schoolClass = schoolClass;
        this.assessmentType = assessmentType == null ? "QUIZ" : assessmentType.trim();
        this.totalMarks = totalMarks == null ? BigDecimal.ZERO : totalMarks;
        this.assessmentDate = assessmentDate;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
