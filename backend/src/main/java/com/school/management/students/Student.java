package com.school.management.students;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "students",
    indexes = {
        @Index(name = "ix_students_admission_number", columnList = "admission_number", unique = true),
        @Index(name = "ix_students_status", columnList = "status"),
        @Index(name = "ix_students_last_name", columnList = "last_name")
    })
public class Student {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "admission_number", nullable = false, length = 80, unique = true)
    private String admissionNumber;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "middle_name", length = 120)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 30)
    private String gender;

    @Column(length = 120)
    private String nationality;

    @Column(length = 120)
    private String religion;

    @Column(length = 30)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Student() {
    }

    public Student(String admissionNumber, String firstName, String middleName, String lastName,
                   LocalDate dateOfBirth, String gender, String nationality, String religion,
                   String phone, String email, String address, String status) {
        this.admissionNumber = admissionNumber.trim();
        this.firstName = firstName.trim();
        this.middleName = middleName == null ? "" : middleName.trim();
        this.lastName = lastName.trim();
        this.dateOfBirth = dateOfBirth;
        this.gender = gender == null ? "UNKNOWN" : gender.trim();
        this.nationality = nationality == null ? "" : nationality.trim();
        this.religion = religion == null ? "" : religion.trim();
        this.phone = phone == null ? "" : phone.trim();
        this.email = email == null ? "" : email.trim();
        this.address = address == null ? "" : address.trim();
        this.status = status == null || status.isBlank() ? "ACTIVE" : status.trim();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateFrom(UpdateStudentRequest request) {
        if (request.status() != null && !request.status().isBlank()) {
            this.status = request.status().trim();
        }
        if (request.address() != null && !request.address().isBlank()) {
            this.address = request.address().trim();
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            this.phone = request.phone().trim();
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getNationality() {
        return nationality;
    }

    public String getReligion() {
        return religion;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
