package com.school.management.academics;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.students.Student;
import com.school.management.students.StudentRepository;

@Service
public class AssessmentService {
    private final SubjectRepository subjects;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final AssessmentRepository assessments;
    private final AssessmentScoreRepository assessmentScores;
    private final StudentRepository students;

    public AssessmentService(SubjectRepository subjects,
                            AcademicYearRepository academicYears,
                            SchoolClassRepository schoolClasses,
                            AssessmentRepository assessments,
                            AssessmentScoreRepository assessmentScores,
                            StudentRepository students) {
        this.subjects = subjects;
        this.academicYears = academicYears;
        this.schoolClasses = schoolClasses;
        this.assessments = assessments;
        this.assessmentScores = assessmentScores;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponse> listAssessments(Pageable pageable) {
        return assessments.findAll(pageable).map(AssessmentResponse::from);
    }

    @Transactional
    public AssessmentResponse createAssessment(CreateAssessmentRequest request) {
        Subject subject = subjects.findById(request.subjectId())
            .orElseThrow(() -> new IllegalArgumentException("Subject was not found."));

        AcademicYear academicYear = academicYears.findById(request.academicYearId())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));

        SchoolClass schoolClass = schoolClasses.findById(request.schoolClassId())
            .orElseThrow(() -> new IllegalArgumentException("School class was not found."));

        String assessmentType = request.assessmentType().trim();
        if (assessments.existsBySubjectIdAndAcademicYearIdAndSchoolClassIdAndAssessmentType(
            request.subjectId(), request.academicYearId(), request.schoolClassId(), assessmentType)) {
            throw new IllegalArgumentException("An assessment of that type already exists for this subject, class and academic year.");
        }

        Assessment assessment = new Assessment(
            subject,
            academicYear,
            schoolClass,
            assessmentType,
            request.totalMarks(),
            request.assessmentDate(),
            request.dueDate()
        );
        return AssessmentResponse.from(assessments.save(assessment));
    }

    @Transactional
    public AssessmentScore recordScore(CreateAssessmentScoreRequest request) {
        Student student = students.findById(request.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));

        Assessment assessment = assessments.findById(request.assessmentId())
            .orElseThrow(() -> new IllegalArgumentException("Assessment was not found."));

        if (assessmentScores.existsByAssessmentIdAndStudentId(request.assessmentId(), request.studentId())) {
            throw new IllegalArgumentException("A score for this student already exists for this assessment.");
        }

        if (request.score().compareTo(assessment.getTotalMarks()) > 0) {
            throw new IllegalArgumentException("Score cannot exceed the assessment total marks.");
        }

        return assessmentScores.save(new AssessmentScore(assessment, student, request.score()));
    }
}
