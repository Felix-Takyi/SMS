package com.school.management.admissions;

import java.time.LocalDate;
import java.util.UUID;

public record AdmissionApplicationResponse(
    UUID id,
    String applicationNumber,
    String firstName,
    String middleName,
    String lastName,
    LocalDate dateOfBirth,
    String gender,
    String nationality,
    String phone,
    String email,
    String address,
    String status
) {
    public static AdmissionApplicationResponse from(AdmissionApplication application) {
        return new AdmissionApplicationResponse(
            application.getId(),
            application.getApplicationNumber(),
            application.getFirstName(),
            application.getMiddleName(),
            application.getLastName(),
            application.getDateOfBirth(),
            application.getGender(),
            application.getNationality(),
            application.getPhone(),
            application.getEmail(),
            application.getAddress(),
            application.getStatus()
        );
    }
}
