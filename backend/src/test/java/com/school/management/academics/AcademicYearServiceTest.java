package com.school.management.academics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AcademicYearServiceTest {
    private final AcademicYearRepository years = mock(AcademicYearRepository.class);
    private final TermRepository terms = mock(TermRepository.class);
    private final AcademicYearService service = new AcademicYearService(years, terms);

    @Test
    void createAcademicYearRejectsDuplicateCode() {
        when(years.existsByCodeIgnoreCase("2026/2027")).thenReturn(true);

        CreateAcademicYearRequest request = new CreateAcademicYearRequest(
            "2026/2027",
            "2026/2027 Academic Year",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            true
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createAcademicYear(request));

        assertEquals("An academic year with that code already exists.", exception.getMessage());
    }

    @Test
    void createTermUsesYearReference() {
        AcademicYear academicYear = new AcademicYear(
            "2027/2028",
            "2027/2028 Academic Year",
            LocalDate.of(2027, 9, 1),
            LocalDate.of(2028, 7, 31),
            true
        );
        when(years.findByCodeIgnoreCase("2027/2028")).thenReturn(Optional.of(academicYear));
        when(terms.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TermResponse response = service.createTerm(new CreateTermRequest(
            "2027/2028",
            "Term 1",
            LocalDate.of(2027, 9, 1),
            LocalDate.of(2027, 12, 15)
        ));

        assertEquals("Term 1", response.name());
    }

    @Test
    void listAcademicYearsReturnsMappedPage() {
        AcademicYear year = new AcademicYear(
            "2028/2029",
            "2028/2029 Academic Year",
            LocalDate.of(2028, 9, 1),
            LocalDate.of(2029, 7, 31),
            false
        );
        when(years.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(year)));

        Page<AcademicYearResponse> page = service.listAcademicYears(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("2028/2029", page.getContent().getFirst().code());
    }
}
