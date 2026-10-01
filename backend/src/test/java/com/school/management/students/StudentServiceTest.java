package com.school.management.students;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final StudentService studentService = new StudentService(students);

    @Test
    void createStudentRejectsDuplicateAdmissionNumber() {
        when(students.existsByAdmissionNumberIgnoreCase("ADM-2026-001")).thenReturn(true);

        CreateStudentRequest request = new CreateStudentRequest(
            "ADM-2026-001",
            "Ama",
            "Serwaa",
            "Mensah",
            LocalDate.of(2014, 6, 15),
            "FEMALE",
            "Ghanaian",
            "Christian",
            "0240000000",
            "ama@example.com",
            "Accra",
            "ACTIVE"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> studentService.createStudent(request));

        assertEquals("A student with that admission number already exists.", exception.getMessage());
    }

    @Test
    void updateStudentChangesStatusAndSaves() {
        UUID studentId = UUID.randomUUID();
        Student student = new Student(
            "ADM-2026-002",
            "Kofi",
            "",
            "Aboagye",
            LocalDate.of(2013, 2, 10),
            "MALE",
            "Ghanaian",
            "Christian",
            "0241111111",
            "kofi@example.com",
            "Kumasi",
            "ACTIVE"
        );
        when(students.findById(studentId)).thenReturn(Optional.of(student));
        when(students.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = studentService.updateStudent(studentId,
            new UpdateStudentRequest("TRANSFERRED", "Northern Region", "0202222222"));

        assertEquals("TRANSFERRED", response.status());
        assertEquals("Northern Region", response.address());
        assertEquals("0202222222", response.phone());
    }

    @Test
    void listStudentsReturnsPageOfResponses() {
        Student student = new Student(
            "ADM-2026-003",
            "Abena",
            "",
            "Owusu",
            LocalDate.of(2015, 9, 21),
            "FEMALE",
            "Ghanaian",
            "Christian",
            "0243333333",
            "abena@example.com",
            "Tema",
            "ACTIVE"
        );
        when(students.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(student)));

        Page<StudentResponse> page = studentService.listStudents(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("ADM-2026-003", page.getContent().getFirst().admissionNumber());
    }
}
