package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class SubjectServiceTest {
    private final SubjectRepository subjects = mock(SubjectRepository.class);
    private final SubjectService service = new SubjectService(subjects);

    @Test
    void createSubjectRejectsDuplicateCode() {
        when(subjects.existsByCodeIgnoreCase("MATH")).thenReturn(true);

        CreateSubjectRequest request = new CreateSubjectRequest(
            "MATH",
            "Mathematics",
            "Core subject",
            true
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createSubject(request));

        assertEquals("A subject with that code already exists.", exception.getMessage());
    }

    @Test
    void listSubjectsReturnsMappedPage() {
        Subject subject = new Subject("MATH", "Mathematics", "Core subject", true);
        when(subjects.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(subject)));

        Page<SubjectResponse> page = service.listSubjects(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("MATH", page.getContent().getFirst().code());
    }
}
