package com.school.management.admissions;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionApplicationService {
    private final AdmissionApplicationRepository applications;

    public AdmissionApplicationService(AdmissionApplicationRepository applications) {
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public Page<AdmissionApplicationResponse> listApplications(Pageable pageable) {
        return applications.findAll(pageable).map(AdmissionApplicationResponse::from);
    }

    @Transactional
    public AdmissionApplicationResponse createApplication(CreateAdmissionApplicationRequest request) {
        String applicationNumber = request.applicationNumber().trim();
        if (applications.existsByApplicationNumberIgnoreCase(applicationNumber)) {
            throw new IllegalArgumentException("A student application with that application number already exists.");
        }

        AdmissionApplication application = new AdmissionApplication(
            applicationNumber,
            request.firstName(),
            request.middleName(),
            request.lastName(),
            request.dateOfBirth(),
            request.gender(),
            request.nationality(),
            request.phone(),
            request.email(),
            request.address(),
            request.status()
        );
        return AdmissionApplicationResponse.from(applications.save(application));
    }

    @Transactional
    public AdmissionApplicationResponse updateStatus(UUID applicationId, String status) {
        AdmissionApplication application = applications.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Admission application was not found."));
        application.updateStatus(status);
        return AdmissionApplicationResponse.from(applications.save(application));
    }
}
