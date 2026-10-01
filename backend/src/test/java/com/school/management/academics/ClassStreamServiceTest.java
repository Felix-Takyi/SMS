package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ClassStreamServiceTest {
    private final SchoolClassRepository schoolClasses = mock(SchoolClassRepository.class);
    private final ClassStreamRepository classStreams = mock(ClassStreamRepository.class);
    private final ClassStreamService service = new ClassStreamService(schoolClasses, classStreams);

    @Test
    void createClassStreamRejectsDuplicateNameForClass() {
        UUID schoolClassId = UUID.randomUUID();
        when(schoolClasses.findById(schoolClassId)).thenReturn(java.util.Optional.of(new SchoolClass("JSS1", "Junior Secondary One", true)));
        when(classStreams.existsBySchoolClassIdAndNameIgnoreCase(schoolClassId, "Science")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createClassStream(new CreateClassStreamRequest(schoolClassId, "Science", true)));

        assertEquals("A stream with that name already exists for this class.", exception.getMessage());
    }

    @Test
    void listClassStreamsReturnsMappedPage() {
        SchoolClass schoolClass = new SchoolClass("JSS2", "Junior Secondary Two", true);
        ClassStream classStream = new ClassStream(schoolClass, "Arts", true);
        when(classStreams.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(classStream)));

        Page<ClassStreamResponse> page = service.listClassStreams(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("Arts", page.getContent().getFirst().name());
    }
}
