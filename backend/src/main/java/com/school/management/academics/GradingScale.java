package com.school.management.academics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "grading_scales")
public class GradingScale {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @OneToMany(mappedBy = "gradingScale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minimumPercentage DESC")
    private List<GradeBand> bands = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GradingScale() {
    }

    public GradingScale(String name, List<CreateGradeBandRequest> bandRequests) {
        this.name = name.trim();
        this.createdAt = Instant.now();
        for (CreateGradeBandRequest band : bandRequests) {
            bands.add(new GradeBand(this, band.gradeLabel(), band.minimumPercentage(),
                band.maximumPercentage(), band.remark()));
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<GradeBand> getBands() {
        return List.copyOf(bands);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
