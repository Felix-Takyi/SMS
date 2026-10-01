package com.school.management.academics;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAttendanceSessionRequest(
    @NotNull UUID academicYearId,
    @NotNull UUID schoolClassId,
    @NotNull LocalDate attendanceDate,
    @NotBlank @Size(max = 80) String sessionName
) {
}
