package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AcademicController {
    private final AcademicYearService academicYearService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;
    private final ClassStreamService classStreamService;

    public AcademicController(AcademicYearService academicYearService,
                             SchoolClassService schoolClassService,
                             SubjectService subjectService,
                             ClassStreamService classStreamService) {
        this.academicYearService = academicYearService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
        this.classStreamService = classStreamService;
    }

    @GetMapping("/academic-years")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public Page<AcademicYearResponse> listAcademicYears(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return academicYearService.listAcademicYears(PageRequest.of(page, size, Sort.by("startDate").ascending()));
    }

    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public AcademicYearResponse createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest request) {
        return academicYearService.createAcademicYear(request);
    }

    @GetMapping("/academic-classes")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public Page<SchoolClassResponse> listSchoolClasses(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return schoolClassService.listClasses(PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    @PostMapping("/academic-classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public SchoolClassResponse createSchoolClass(@Valid @RequestBody CreateSchoolClassRequest request) {
        return schoolClassService.createSchoolClass(request);
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public Page<SubjectResponse> listSubjects(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return subjectService.listSubjects(PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public SubjectResponse createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return subjectService.createSubject(request);
    }

    @GetMapping("/academic-classes/{classId}/streams")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public Page<ClassStreamResponse> listClassStreams(@PathVariable UUID classId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return classStreamService.listClassStreamsForSchoolClass(classId, PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    @PostMapping("/class-streams")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public ClassStreamResponse createClassStream(@Valid @RequestBody CreateClassStreamRequest request) {
        return classStreamService.createClassStream(request);
    }

    @GetMapping("/academic-years/{code}/terms")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public List<TermResponse> listTerms(@PathVariable String code) {
        return academicYearService.listTermsForAcademicYear(code);
    }

    @PostMapping("/terms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public TermResponse createTerm(@Valid @RequestBody CreateTermRequest request) {
        return academicYearService.createTerm(request);
    }
}
