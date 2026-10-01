package com.school.management.academics;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateGradingScaleRequest(
    @NotBlank @Size(max = 120) String name,
    @NotEmpty @Valid List<CreateGradeBandRequest> bands
) {
}
