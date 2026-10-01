package com.school.management.students;

import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
    @Size(max = 40) String status,
    @Size(max = 255) String address,
    @Size(max = 30) String phone
) {
}
