package com.school.management.students;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> listStudents(Pageable pageable) {
        return students.findAll(pageable).map(StudentResponse::from);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID studentId) {
        Student student = students.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));
        return StudentResponse.from(student);
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        String admissionNumber = request.admissionNumber().trim();
        if (students.existsByAdmissionNumberIgnoreCase(admissionNumber)) {
            throw new IllegalArgumentException("A student with that admission number already exists.");
        }

        Student student = new Student(
            admissionNumber,
            request.firstName(),
            request.middleName(),
            request.lastName(),
            request.dateOfBirth(),
            request.gender(),
            request.nationality(),
            request.religion(),
            request.phone(),
            request.email(),
            request.address(),
            request.status()
        );
        return StudentResponse.from(students.save(student));
    }

    @Transactional
    public StudentResponse updateStudent(UUID studentId, UpdateStudentRequest request) {
        Student student = students.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));
        student.updateFrom(request);
        return StudentResponse.from(students.save(student));
    }
}
