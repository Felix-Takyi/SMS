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

class SchoolClassServiceTest {
    private final SchoolClassRepository schoolClasses = mock(SchoolClassRepository.class);
    private final SchoolClassService service = new SchoolClassService(schoolClasses);

    @Test
    void createSchoolClassRejectsDuplicateCode() {
        when(schoolClasses.existsByCodeIgnoreCase("JSS1")).thenReturn(true);

        CreateSchoolClassRequest request = new CreateSchoolClassRequest(
            "JSS1",
            "Junior Secondary One",
            true
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createSchoolClass(request));

        assertEquals("A class with that code already exists.", exception.getMessage());
    }

    @Test
    void listSchoolClassesReturnsMappedPage() {
        SchoolClass schoolClass = new SchoolClass("JSS1", "Junior Secondary One", true);
        when(schoolClasses.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(schoolClass)));

        Page<SchoolClassResponse> page = service.listClasses(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("JSS1", page.getContent().getFirst().code());
    }
}
