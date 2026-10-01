package com.school.management.students;

import java.time.LocalDate;
import java.util.UUID;

public record StudentResponse(
    UUID id,
    String admissionNumber,
    String firstName,
    String middleName,
    String lastName,
    LocalDate dateOfBirth,
    String gender,
    String nationality,
    String religion,
    String phone,
    String email,
    String address,
    String status
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getAdmissionNumber(),
            student.getFirstName(),
            student.getMiddleName(),
            student.getLastName(),
            student.getDateOfBirth(),
            student.getGender(),
            student.getNationality(),
            student.getReligion(),
            student.getPhone(),
            student.getEmail(),
            student.getAddress(),
            student.getStatus()
        );
    }
}
