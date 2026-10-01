package com.school.management.students;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.school.management.admissions.AdmissionApplication;
import com.school.management.admissions.AdmissionApplicationRepository;
import com.school.management.admissions.AdmissionApplicationResponse;
import com.school.management.admissions.AdmissionApplicationService;
import com.school.management.admissions.CreateAdmissionApplicationRequest;

class AdmissionApplicationServiceTest {
    private final AdmissionApplicationRepository applications = mock(AdmissionApplicationRepository.class);
    private final AdmissionApplicationService service = new AdmissionApplicationService(applications);

    @Test
    void createAdmissionApplicationRejectsDuplicateApplicationNumber() {
        when(applications.existsByApplicationNumberIgnoreCase("APP-2026-001")).thenReturn(true);

        CreateAdmissionApplicationRequest request = new CreateAdmissionApplicationRequest(
            "APP-2026-001",
            "Ama",
            "Mensah",
            "Adu",
            LocalDate.of(2014, 6, 15),
            "FEMALE",
            "Ghanaian",
            "0240000000",
            "ama@example.com",
            "Accra",
            "PENDING"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createApplication(request));

        assertEquals("A student application with that application number already exists.", exception.getMessage());
    }

    @Test
    void createAdmissionApplicationGeneratesResponse() {
        when(applications.existsByApplicationNumberIgnoreCase("APP-2026-002")).thenReturn(false);
        when(applications.save(any(AdmissionApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAdmissionApplicationRequest request = new CreateAdmissionApplicationRequest(
            "APP-2026-002",
            "Kofi",
            "Aboagye",
            "",
            LocalDate.of(2013, 2, 10),
            "MALE",
            "Ghanaian",
            "0241111111",
            "kofi@example.com",
            "Kumasi",
            "PENDING"
        );

        AdmissionApplicationResponse response = service.createApplication(request);

        assertEquals("APP-2026-002", response.applicationNumber());
        assertEquals("PENDING", response.status());
    }

    @Test
    void updateStatusChangesApplicationStatus() {
        AdmissionApplication application = new AdmissionApplication(
            "APP-2026-003",
            "Abena",
            "Owusu",
            "",
            LocalDate.of(2015, 9, 21),
            "FEMALE",
            "Ghanaian",
            "0243333333",
            "abena@example.com",
            "Tema",
            "PENDING"
        );
        when(applications.findById(any())).thenReturn(Optional.of(application));
        when(applications.save(any(AdmissionApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplicationResponse response = service.updateStatus(java.util.UUID.randomUUID(), "APPROVED");

        assertEquals("APPROVED", response.status());
    }
}
