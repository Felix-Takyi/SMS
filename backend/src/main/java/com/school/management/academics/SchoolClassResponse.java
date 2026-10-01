package com.school.management.academics;

import java.util.UUID;

public record SchoolClassResponse(
    UUID id,
    String code,
    String name,
    boolean active
) {
    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(
            schoolClass.getId(),
            schoolClass.getCode(),
            schoolClass.getName(),
            schoolClass.isActive()
        );
    }
}
