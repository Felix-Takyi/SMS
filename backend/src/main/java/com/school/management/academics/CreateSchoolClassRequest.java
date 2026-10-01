package com.school.management.academics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
    @NotBlank @Size(max = 40) String code,
    @NotBlank @Size(max = 150) String name,
    boolean active
) {
}
