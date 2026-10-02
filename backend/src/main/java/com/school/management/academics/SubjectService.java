package com.school.management.academics;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {
    private final SubjectRepository subjects;
    private final AssessmentRepository assessments;

    @Autowired
    public SubjectService(SubjectRepository subjects, AssessmentRepository assessments) {
        this.subjects = subjects;
        this.assessments = assessments;
    }

    SubjectService(SubjectRepository subjects) {
        this(subjects, null);
    }

    @Transactional(readOnly = true)
    public Page<SubjectResponse> listSubjects(Pageable pageable) {
        return subjects.findAll(pageable).map(SubjectResponse::from);
    }

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        String code = request.code().trim();
        if (subjects.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A subject with that code already exists.");
        }
        Subject subject = new Subject(code, request.name(), request.description(), request.active());
        return SubjectResponse.from(subjects.save(subject));
    }

    @Transactional
    public SubjectResponse updateSubject(UUID id, CreateSubjectRequest request) {
        Subject subject = subjects.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject was not found."));
        String code = request.code().trim();
        if (subjects.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("A subject with that code already exists.");
        }
        subject.update(code, request.name(), request.description(), request.active());
        return SubjectResponse.from(subject);
    }

    @Transactional
    public void deleteSubject(UUID subjectId) {
        if (!subjects.existsById(subjectId)) {
            throw new IllegalArgumentException("Subject was not found.");
        }
        if (assessments != null && assessments.existsBySubjectId(subjectId)) {
            throw new IllegalArgumentException("This subject has assessments. Remove those assessments before deleting the subject.");
        }
        subjects.deleteById(subjectId);
    }
}
