package com.school.management.academics;

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

    @Transactional(readOnly = true)
    public Page<AssessmentScoreResponse> listScores(UUID assessmentId, Pageable pageable) {
        if (!assessments.existsById(assessmentId)) {
            throw new IllegalArgumentException("Assessment was not found.");
        }
        return assessmentScores.findByAssessmentId(assessmentId, pageable).map(AssessmentScoreResponse::from);
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
        if (request.totalMarks().signum() <= 0) {
            throw new IllegalArgumentException("Assessment total marks must be greater than zero.");
        }
        if (request.dueDate().isBefore(request.assessmentDate())) {
            throw new IllegalArgumentException("Due date cannot be before the assessment date.");
        }

        Assessment assessment = new Assessment(subject, academicYear, schoolClass, assessmentType,
            request.totalMarks(), request.assessmentDate(), request.dueDate());
        return AssessmentResponse.from(assessments.save(assessment));
    }

    @Transactional
    public AssessmentScoreResponse recordScore(CreateAssessmentScoreRequest request) {
        Student student = students.findById(request.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));
        Assessment assessment = assessments.findById(request.assessmentId())
            .orElseThrow(() -> new IllegalArgumentException("Assessment was not found."));
        if (assessmentScores.existsByAssessmentIdAndStudentId(request.assessmentId(), request.studentId())) {
            throw new IllegalArgumentException("A score for this student already exists for this assessment.");
        }
        validateScore(request.score(), assessment);
        return AssessmentScoreResponse.from(assessmentScores.save(new AssessmentScore(assessment, student, request.score())));
    }

    @Transactional
    public AssessmentScoreResponse updateScore(UUID scoreId, UpdateAssessmentScoreRequest request) {
        AssessmentScore score = assessmentScores.findById(scoreId)
            .orElseThrow(() -> new IllegalArgumentException("Assessment score was not found."));
        validateScore(request.score(), score.getAssessment());
        score.updateScore(request.score());
        return AssessmentScoreResponse.from(score);
    }

    @Transactional
    public void deleteScore(UUID scoreId) {
        if (!assessmentScores.existsById(scoreId)) {
            throw new IllegalArgumentException("Assessment score was not found.");
        }
        assessmentScores.deleteById(scoreId);
    }

    @Transactional
    public void deleteAssessment(UUID assessmentId) {
        if (!assessments.existsById(assessmentId)) {
            throw new IllegalArgumentException("Assessment was not found.");
        }
        assessmentScores.deleteByAssessmentId(assessmentId);
        assessments.deleteById(assessmentId);
    }

    private void validateScore(java.math.BigDecimal score, Assessment assessment) {
        if (score == null || score.signum() < 0) {
            throw new IllegalArgumentException("Score cannot be negative.");
        }
        if (score.compareTo(assessment.getTotalMarks()) > 0) {
            throw new IllegalArgumentException("Score cannot exceed the assessment total marks.");
        }
    }
}
