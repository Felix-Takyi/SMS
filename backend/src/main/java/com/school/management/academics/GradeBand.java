package com.school.management.academics;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "grade_bands")
public class GradeBand {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_scale_id", nullable = false)
    private GradingScale gradingScale;

    @Column(name = "grade_label", nullable = false, length = 20)
    private String gradeLabel;

    @Column(name = "minimum_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumPercentage;

    @Column(name = "maximum_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maximumPercentage;

    @Column(length = 300)
    private String remark;

    protected GradeBand() {
    }

    public GradeBand(GradingScale gradingScale, String gradeLabel, BigDecimal minimumPercentage,
                     BigDecimal maximumPercentage, String remark) {
        this.gradingScale = gradingScale;
        this.gradeLabel = gradeLabel.trim();
        this.minimumPercentage = minimumPercentage;
        this.maximumPercentage = maximumPercentage;
        this.remark = remark == null ? null : remark.trim();
    }

    public UUID getId() {
        return id;
    }

    public GradingScale getGradingScale() {
        return gradingScale;
    }

    public String getGradeLabel() {
        return gradeLabel;
    }

    public BigDecimal getMinimumPercentage() {
        return minimumPercentage;
    }

    public BigDecimal getMaximumPercentage() {
        return maximumPercentage;
    }

    public String getRemark() {
        return remark;
    }
}
