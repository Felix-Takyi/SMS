package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.school.management.students.Student;
import com.school.management.students.StudentRepository;

class StudentEnrollmentServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final AcademicYearRepository academicYears = mock(AcademicYearRepository.class);
    private final SchoolClassRepository schoolClasses = mock(SchoolClassRepository.class);
    private final ClassStreamRepository classStreams = mock(ClassStreamRepository.class);
    private final StudentEnrollmentRepository enrollments = mock(StudentEnrollmentRepository.class);
    private final StudentEnrollmentService service = new StudentEnrollmentService(
        students, academicYears, schoolClasses, classStreams, enrollments
    );

    @Test
    void createEnrollmentRejectsDuplicateActiveYearEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        UUID schoolClassId = UUID.randomUUID();
        Student student = new Student(
            "ADM-2026-010",
            "Amina",
            "",
            "Boateng",
            LocalDate.of(2014, 5, 15),
            "FEMALE",
            "Ghanaian",
            "Christian",
            "0241234567",
            "amina@example.com",
            "Cape Coast",
            "ACTIVE"
        );
        AcademicYear academicYear = new AcademicYear(
            "2026/2027",
            "2026/2027 Academic Year",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            true
        );
        SchoolClass schoolClass = new SchoolClass("JSS1", "Junior Secondary One", true);

        when(students.findById(studentId)).thenReturn(Optional.of(student));
        when(academicYears.findById(academicYearId)).thenReturn(Optional.of(academicYear));
        when(schoolClasses.findById(schoolClassId)).thenReturn(Optional.of(schoolClass));
        when(enrollments.existsByStudentIdAndAcademicYearIdAndStatus(studentId, academicYearId, "ACTIVE")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createEnrollment(new CreateStudentEnrollmentRequest(
                studentId,
                academicYearId,
                schoolClassId,
                null,
                "ACTIVE"
            )));

        assertEquals("This student already has an active enrollment for the academic year.", exception.getMessage());
    }

    @Test
    void listEnrollmentsReturnsMappedPage() {
        UUID studentId = UUID.randomUUID();
        Student student = new Student(
            "ADM-2026-011",
            "Kwame",
            "",
            "Asare",
            LocalDate.of(2013, 8, 12),
            "MALE",
            "Ghanaian",
            "Christian",
            "0247654321",
            "kwame@example.com",
            "Kumasi",
            "ACTIVE"
        );
        AcademicYear academicYear = new AcademicYear(
            "2026/2027",
            "2026/2027 Academic Year",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            true
        );
        SchoolClass schoolClass = new SchoolClass("JSS2", "Junior Secondary Two", true);
        StudentEnrollment enrollment = new StudentEnrollment(student, academicYear, schoolClass, null, "ACTIVE");
        when(enrollments.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(enrollment)));

        Page<StudentEnrollmentResponse> page = service.listEnrollments(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("ACTIVE", page.getContent().getFirst().status());
    }
}
