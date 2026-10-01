package com.school.management.academics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
    @NotBlank @Size(max = 40) String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 500) String description,
    boolean active
) {
}
