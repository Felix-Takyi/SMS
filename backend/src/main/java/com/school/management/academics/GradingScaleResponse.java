package com.school.management.academics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GradingScaleResponse(UUID id, String name, List<GradeBandResponse> bands, Instant createdAt) {
    public static GradingScaleResponse from(GradingScale gradingScale) {
        return new GradingScaleResponse(
            gradingScale.getId(),
            gradingScale.getName(),
            gradingScale.getBands().stream().map(GradeBandResponse::from).toList(),
            gradingScale.getCreatedAt()
        );
    }
}
