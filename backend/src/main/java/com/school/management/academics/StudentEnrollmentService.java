package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.students.Student;
import com.school.management.students.StudentRepository;

@Service
public class StudentEnrollmentService {
    private final StudentRepository students;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final ClassStreamRepository classStreams;
    private final StudentEnrollmentRepository enrollments;

    public StudentEnrollmentService(StudentRepository students,
                                   AcademicYearRepository academicYears,
                                   SchoolClassRepository schoolClasses,
                                   ClassStreamRepository classStreams,
                                   StudentEnrollmentRepository enrollments) {
        this.students = students;
        this.academicYears = academicYears;
        this.schoolClasses = schoolClasses;
        this.classStreams = classStreams;
        this.enrollments = enrollments;
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrollmentResponse> listEnrollments(Pageable pageable) {
        return enrollments.findAll(pageable).map(StudentEnrollmentResponse::from);
    }

    @Transactional
    public StudentEnrollmentResponse createEnrollment(CreateStudentEnrollmentRequest request) {
        Student student = students.findById(request.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));

        AcademicYear academicYear = academicYears.findById(request.academicYearId())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));

        SchoolClass schoolClass = schoolClasses.findById(request.schoolClassId())
            .orElseThrow(() -> new IllegalArgumentException("School class was not found."));

        ClassStream classStream = null;
        if (request.classStreamId() != null) {
            classStream = classStreams.findById(request.classStreamId())
                .orElseThrow(() -> new IllegalArgumentException("Class stream was not found."));
            if (!classStream.getSchoolClass().getId().equals(schoolClass.getId())) {
                throw new IllegalArgumentException("The selected stream does not belong to the selected class.");
            }
        }

        String status = request.status().trim();
        if (enrollments.existsByStudentIdAndAcademicYearIdAndStatus(request.studentId(), request.academicYearId(), "ACTIVE")) {
            throw new IllegalArgumentException("This student already has an active enrollment for the academic year.");
        }

        StudentEnrollment enrollment = new StudentEnrollment(student, academicYear, schoolClass, classStream, status);
        return StudentEnrollmentResponse.from(enrollments.save(enrollment));
    }

    @Transactional
    public StudentEnrollmentResponse updateEnrollment(UUID id, CreateStudentEnrollmentRequest request) {
        StudentEnrollment enrollment = enrollments.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Student enrollment was not found."));
        Student student = students.findById(request.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));
        AcademicYear academicYear = academicYears.findById(request.academicYearId())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        SchoolClass schoolClass = schoolClasses.findById(request.schoolClassId())
            .orElseThrow(() -> new IllegalArgumentException("School class was not found."));
        ClassStream classStream = request.classStreamId() == null ? null : classStreams.findById(request.classStreamId())
            .orElseThrow(() -> new IllegalArgumentException("Class stream was not found."));
        if (classStream != null && !classStream.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw new IllegalArgumentException("The selected stream does not belong to the selected class.");
        }

        String status = request.status().trim();
        if ("ACTIVE".equalsIgnoreCase(status)
                && enrollments.existsByStudentIdAndAcademicYearIdAndStatusAndIdNot(
                    student.getId(), academicYear.getId(), "ACTIVE", id)) {
            throw new IllegalArgumentException("This student already has an active enrollment for the academic year.");
        }
        enrollment.update(student, academicYear, schoolClass, classStream, status);
        return StudentEnrollmentResponse.from(enrollment);
    }
}
