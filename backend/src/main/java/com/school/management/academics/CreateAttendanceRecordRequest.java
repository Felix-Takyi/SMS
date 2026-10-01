package com.school.management.academics;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAttendanceRecordRequest(
    @NotNull UUID attendanceSessionId,
    @NotNull UUID studentId,
    @NotBlank @Size(max = 20) String status,
    @Size(max = 255) String note
) {
}
