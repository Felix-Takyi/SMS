package com.school.management.academics;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTermRequest(
    @NotBlank @Size(max = 40) String academicYearCode,
    @NotBlank @Size(max = 80) String name,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    boolean active
) {
}