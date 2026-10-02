package com.school.management.academics;

import java.math.BigDecimal;
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
@Table(name = "assessment_scores",
    indexes = {
        @Index(name = "ix_assessment_scores_assessment_student", columnList = "assessment_id, student_id", unique = true),
        @Index(name = "ix_assessment_scores_student", columnList = "student_id")
    })
public class AssessmentScore {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AssessmentScore() {
    }

    public AssessmentScore(Assessment assessment, Student student, BigDecimal score) {
        this.assessment = assessment;
        this.student = student;
        this.score = score == null ? BigDecimal.ZERO : score;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public Student getStudent() {
        return student;
    }

    public BigDecimal getScore() {
        return score;
    }


    public void updateScore(BigDecimal score) {
        this.score = score == null ? BigDecimal.ZERO : score;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
