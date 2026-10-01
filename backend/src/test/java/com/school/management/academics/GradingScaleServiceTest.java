package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GradingScaleServiceTest {
    private final GradingScaleRepository gradingScales = mock(GradingScaleRepository.class);
    private final GradingScaleService service = new GradingScaleService(gradingScales);

    @Test
    void createGradingScaleRejectsOverlappingBands() {
        CreateGradingScaleRequest request = new CreateGradingScaleRequest(
            "Senior Secondary",
            List.of(
                new CreateGradeBandRequest("A", new BigDecimal("80"), new BigDecimal("100"), "Excellent"),
                new CreateGradeBandRequest("B", new BigDecimal("70"), new BigDecimal("85"), "Very good")
            )
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createGradingScale(request));

        assertEquals("Grade bands must not overlap.", exception.getMessage());
    }

    @Test
    void findGradeReturnsMatchingConfiguredBand() {
        UUID gradingScaleId = UUID.randomUUID();
        GradingScale gradingScale = new GradingScale("Senior Secondary", List.of(
            new CreateGradeBandRequest("A", new BigDecimal("80"), new BigDecimal("100"), "Excellent"),
            new CreateGradeBandRequest("B", new BigDecimal("70"), new BigDecimal("79.99"), "Very good")
        ));
        when(gradingScales.findById(gradingScaleId)).thenReturn(Optional.of(gradingScale));

        GradeBandResponse grade = service.findGrade(gradingScaleId, new BigDecimal("79.99"));

        assertEquals("B", grade.gradeLabel());
    }
}
