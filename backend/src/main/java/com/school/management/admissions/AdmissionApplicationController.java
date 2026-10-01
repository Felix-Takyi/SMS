package com.school.management.admissions;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AdmissionApplicationController {
    private final AdmissionApplicationService admissionApplicationService;

    public AdmissionApplicationController(AdmissionApplicationService admissionApplicationService) {
        this.admissionApplicationService = admissionApplicationService;
    }

    @GetMapping("/admissions")
    @PreAuthorize("hasAuthority('ADMISSION_REVIEW')")
    public Page<AdmissionApplicationResponse> listApplications(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return admissionApplicationService.listApplications(PageRequest.of(page, size, Sort.by("applicationNumber").ascending()));
    }

    @PostMapping("/admissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMISSION_REVIEW')")
    public AdmissionApplicationResponse createApplication(@Valid @RequestBody CreateAdmissionApplicationRequest request) {
        return admissionApplicationService.createApplication(request);
    }

    @PutMapping("/admissions/{id}/status")
    @PreAuthorize("hasAuthority('ADMISSION_APPROVE')")
    public AdmissionApplicationResponse updateStatus(@PathVariable UUID id,
                                                   @RequestBody String status) {
        return admissionApplicationService.updateStatus(id, status.replaceAll("\"", "").trim());
    }
}
