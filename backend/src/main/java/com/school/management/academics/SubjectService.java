package com.school.management.academics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {
    private final SubjectRepository subjects;

    public SubjectService(SubjectRepository subjects) {
        this.subjects = subjects;
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
}
