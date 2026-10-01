package com.school.management.academics;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClassStreamRequest(
    @NotNull UUID schoolClassId,
    @NotBlank @Size(max = 100) String name,
    boolean active
) {
}
