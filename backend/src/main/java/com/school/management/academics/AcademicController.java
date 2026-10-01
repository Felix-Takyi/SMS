package com.school.management.academics;

import java.math.BigDecimal;
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
    private final StudentEnrollmentService studentEnrollmentService;
    private final AttendanceSessionService attendanceSessionService;
    private final AssessmentService assessmentService;
    private final GradingScaleService gradingScaleService;

    public AcademicController(AcademicYearService academicYearService,
                             SchoolClassService schoolClassService,
                             SubjectService subjectService,
                             ClassStreamService classStreamService,
                             StudentEnrollmentService studentEnrollmentService,
                             AttendanceSessionService attendanceSessionService,
                             AssessmentService assessmentService,
                             GradingScaleService gradingScaleService) {
        this.academicYearService = academicYearService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
        this.classStreamService = classStreamService;
        this.studentEnrollmentService = studentEnrollmentService;
        this.attendanceSessionService = attendanceSessionService;
        this.assessmentService = assessmentService;
        this.gradingScaleService = gradingScaleService;
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

    @GetMapping("/student-enrollments")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public Page<StudentEnrollmentResponse> listEnrollments(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return studentEnrollmentService.listEnrollments(PageRequest.of(page, size, Sort.by("enrollmentDate").descending()));
    }

    @PostMapping("/student-enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public StudentEnrollmentResponse createEnrollment(@Valid @RequestBody CreateStudentEnrollmentRequest request) {
        return studentEnrollmentService.createEnrollment(request);
    }

    @GetMapping("/attendance-sessions")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public Page<AttendanceSessionResponse> listAttendanceSessions(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return attendanceSessionService.listAttendanceSessions(PageRequest.of(page, size, Sort.by("attendanceDate").descending()));
    }

    @PostMapping("/attendance-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ATTENDANCE_RECORD')")
    public AttendanceSessionResponse createAttendanceSession(@Valid @RequestBody CreateAttendanceSessionRequest request) {
        return attendanceSessionService.createAttendanceSession(request);
    }

    @GetMapping("/assessments")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public Page<AssessmentResponse> listAssessments(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return assessmentService.listAssessments(PageRequest.of(page, size, Sort.by("assessmentDate").descending()));
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ASSESSMENT_WRITE')")
    public AssessmentResponse createAssessment(@Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.createAssessment(request);
    }

    @PostMapping("/assessment-scores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ASSESSMENT_WRITE')")
    public AssessmentScore recordAssessmentScore(@Valid @RequestBody CreateAssessmentScoreRequest request) {
        return assessmentService.recordScore(request);
    }

    @GetMapping("/grading-scales")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public Page<GradingScaleResponse> listGradingScales(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return gradingScaleService.listGradingScales(PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    @PostMapping("/grading-scales")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ASSESSMENT_WRITE')")
    public GradingScaleResponse createGradingScale(@Valid @RequestBody CreateGradingScaleRequest request) {
        return gradingScaleService.createGradingScale(request);
    }

    @GetMapping("/grading-scales/{gradingScaleId}/grade")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public GradeBandResponse findGrade(@PathVariable UUID gradingScaleId,
                                       @RequestParam BigDecimal percentage) {
        return gradingScaleService.findGrade(gradingScaleId, percentage);
    }

    @PostMapping("/attendance-records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ATTENDANCE_RECORD')")
    public AttendanceRecord recordAttendance(@Valid @RequestBody CreateAttendanceRecordRequest request) {
        return attendanceSessionService.recordAttendance(request);
    }

    @PostMapping("/terms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMICS_MANAGE')")
    public TermResponse createTerm(@Valid @RequestBody CreateTermRequest request) {
        return academicYearService.createTerm(request);
    }
}
