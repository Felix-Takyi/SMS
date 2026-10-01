package com.school.management.admissions;

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
@Table(name = "admission_applications",
    indexes = {
        @Index(name = "ix_admission_applications_number", columnList = "application_number", unique = true),
        @Index(name = "ix_admission_applications_status", columnList = "status"),
        @Index(name = "ix_admission_applications_last_name", columnList = "last_name")
    })
public class AdmissionApplication {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "application_number", nullable = false, length = 80, unique = true)
    private String applicationNumber;

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

    protected AdmissionApplication() {
    }

    public AdmissionApplication(String applicationNumber, String firstName, String middleName,
                               String lastName, LocalDate dateOfBirth, String gender,
                               String nationality, String phone, String email, String address,
                               String status) {
        this.applicationNumber = applicationNumber == null ? "" : applicationNumber.trim();
        this.firstName = firstName == null ? "" : firstName.trim();
        this.middleName = middleName == null ? "" : middleName.trim();
        this.lastName = lastName == null ? "" : lastName.trim();
        this.dateOfBirth = dateOfBirth;
        this.gender = gender == null ? "UNKNOWN" : gender.trim();
        this.nationality = nationality == null ? "" : nationality.trim();
        this.phone = phone == null ? "" : phone.trim();
        this.email = email == null ? "" : email.trim();
        this.address = address == null ? "" : address.trim();
        this.status = status == null || status.isBlank() ? "PENDING" : status.trim();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateStatus(String newStatus) {
        if (newStatus != null && !newStatus.isBlank()) {
            this.status = newStatus.trim();
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getApplicationNumber() {
        return applicationNumber;
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
