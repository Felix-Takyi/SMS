package com.school.management.admissions;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record CreateAdmissionApplicationRequest(
    @NotBlank @Size(max = 80) String applicationNumber,
    @NotBlank @Size(max = 120) String firstName,
    @Size(max = 120) String middleName,
    @NotBlank @Size(max = 120) String lastName,
    @NotNull @Past LocalDate dateOfBirth,
    @NotBlank @Size(max = 30) String gender,
    @Size(max = 120) String nationality,
    @Size(max = 30) String phone,
    @Size(max = 254) String email,
    @Size(max = 255) String address,
    @NotBlank @Size(max = 40) String status
) {
}
