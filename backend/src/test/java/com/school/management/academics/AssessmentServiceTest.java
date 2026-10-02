package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.school.management.students.StudentRepository;
import com.school.management.users.AppUser;
import com.school.management.users.Role;

class AssessmentServiceTest {
    private final SubjectRepository subjects = mock(SubjectRepository.class);
    private final AcademicYearRepository academicYears = mock(AcademicYearRepository.class);
    private final SchoolClassRepository schoolClasses = mock(SchoolClassRepository.class);
    private final AssessmentRepository assessments = mock(AssessmentRepository.class);
    private final AssessmentScoreRepository assessmentScores = mock(AssessmentScoreRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final TeacherSubjectAssignmentRepository teacherAssignments = mock(TeacherSubjectAssignmentRepository.class);
    private final AssessmentService service = new AssessmentService(
        subjects, academicYears, schoolClasses, assessments, assessmentScores, students, teacherAssignments
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCannotCreateAssessmentForUnassignedClassSubjectAndYear() {
        UUID teacherId = UUID.randomUUID();
        AppUser teacher = mock(AppUser.class);
        Role teacherRole = mock(Role.class);
        when(teacher.getId()).thenReturn(teacherId);
        when(teacher.getRoles()).thenReturn(java.util.Set.of(teacherRole));
        when(teacherRole.getCode()).thenReturn("TEACHER");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(teacher, "", List.of()));

        CreateAssessmentRequest request = new CreateAssessmentRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "QUIZ",
            new BigDecimal("20"), LocalDate.now(), LocalDate.now().plusDays(7));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> service.createAssessment(request));
        assertEquals("You can only create assessments for subjects assigned to you in this class and academic year.",
            exception.getMessage());
        verify(teacherAssignments).existsByAcademicYearIdAndSchoolClassIdAndSubjectIdAndTeacher_Id(
            request.academicYearId(), request.schoolClassId(), request.subjectId(), teacherId);
        verifyNoInteractions(subjects, academicYears, schoolClasses, assessments);
    }

    @Test
    void createAssessmentRejectsDuplicateTypeForClassAndSubject() {
        UUID subjectId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        UUID schoolClassId = UUID.randomUUID();
        when(subjects.findById(subjectId)).thenReturn(Optional.of(new Subject("MATH", "Mathematics", "Core", true)));
        when(academicYears.findById(academicYearId)).thenReturn(Optional.of(new AcademicYear(
            "2026/2027",
            "2026/2027 Academic Year",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            true
        )));
        when(schoolClasses.findById(schoolClassId)).thenReturn(Optional.of(new SchoolClass("JSS1", "Junior Secondary One", true)));
        when(assessments.existsBySubjectIdAndAcademicYearIdAndSchoolClassIdAndAssessmentType(
            subjectId, academicYearId, schoolClassId, "QUIZ")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createAssessment(new CreateAssessmentRequest(
                subjectId,
                academicYearId,
                schoolClassId,
                "QUIZ",
                new BigDecimal("20"),
                LocalDate.now(),
                LocalDate.now().plusDays(7)
            )));

        assertEquals("An assessment of that type already exists for this subject, class and academic year.", exception.getMessage());
    }

    @Test
    void recordScoreRejectsUnknownStudent() {
        UUID studentId = UUID.randomUUID();
        when(students.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.recordScore(new CreateAssessmentScoreRequest(
                UUID.randomUUID(),
                studentId,
                new BigDecimal("15")
            )));

        assertEquals("Student was not found.", exception.getMessage());
    }

    @Test
    void listAssessmentsReturnsMappedPage() {
        Assessment assessment = new Assessment(
            new Subject("ENG", "English", "Language", true),
            new AcademicYear("2026/2027", "2026/2027 Academic Year", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 31), true),
            new SchoolClass("JSS2", "Junior Secondary Two", true),
            "TEST",
            new BigDecimal("50"),
            LocalDate.now(),
            LocalDate.now().plusDays(2)
        );
        when(assessments.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(assessment)));

        Page<AssessmentResponse> page = service.listAssessments(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("TEST", page.getContent().getFirst().assessmentType());
    }
}
