package com.school.management.academics;

import java.util.UUID;

public record ClassStreamResponse(
    UUID id,
    UUID schoolClassId,
    String schoolClassCode,
    String name,
    boolean active
) {
    public static ClassStreamResponse from(ClassStream classStream) {
        return new ClassStreamResponse(
            classStream.getId(),
            classStream.getSchoolClass().getId(),
            classStream.getSchoolClass().getCode(),
            classStream.getName(),
            classStream.isActive()
        );
    }
}
