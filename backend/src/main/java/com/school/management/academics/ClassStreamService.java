package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassStreamService {
    private final SchoolClassRepository schoolClasses;
    private final ClassStreamRepository classStreams;

    public ClassStreamService(SchoolClassRepository schoolClasses, ClassStreamRepository classStreams) {
        this.schoolClasses = schoolClasses;
        this.classStreams = classStreams;
    }

    @Transactional(readOnly = true)
    public Page<ClassStreamResponse> listClassStreams(Pageable pageable) {
        return classStreams.findAll(pageable).map(ClassStreamResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ClassStreamResponse> listClassStreamsForSchoolClass(UUID schoolClassId, Pageable pageable) {
        schoolClasses.findById(schoolClassId)
            .orElseThrow(() -> new IllegalArgumentException("School class was not found."));
        return classStreams.findBySchoolClassId(schoolClassId, pageable).map(ClassStreamResponse::from);
    }

    @Transactional
    public ClassStreamResponse createClassStream(CreateClassStreamRequest request) {
        SchoolClass schoolClass = schoolClasses.findById(request.schoolClassId())
            .orElseThrow(() -> new IllegalArgumentException("School class was not found."));

        String name = request.name().trim();
        if (classStreams.existsBySchoolClassIdAndNameIgnoreCase(request.schoolClassId(), name)) {
            throw new IllegalArgumentException("A stream with that name already exists for this class.");
        }

        ClassStream classStream = new ClassStream(schoolClass, name, request.active());
        return ClassStreamResponse.from(classStreams.save(classStream));
    }
}
