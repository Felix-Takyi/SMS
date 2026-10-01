package com.school.management.academics;

import java.util.UUID;

public record SubjectResponse(
    UUID id,
    String code,
    String name,
    String description,
    boolean active
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
            subject.getId(),
            subject.getCode(),
            subject.getName(),
            subject.getDescription(),
            subject.isActive()
        );
    }
}
