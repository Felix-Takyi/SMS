package com.school.management.academics;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingScaleService {
    private final GradingScaleRepository gradingScales;

    public GradingScaleService(GradingScaleRepository gradingScales) {
        this.gradingScales = gradingScales;
    }

    @Transactional(readOnly = true)
    public Page<GradingScaleResponse> listGradingScales(Pageable pageable) {
        return gradingScales.findAll(pageable).map(GradingScaleResponse::from);
    }

    @Transactional(readOnly = true)
    public GradeBandResponse findGrade(UUID gradingScaleId, BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }

        GradingScale gradingScale = gradingScales.findById(gradingScaleId)
            .orElseThrow(() -> new IllegalArgumentException("Grading scale was not found."));

        GradeBand gradeBand = gradingScale.getBands().stream()
            .filter(band -> percentage.compareTo(band.getMinimumPercentage()) >= 0
                && percentage.compareTo(band.getMaximumPercentage()) <= 0)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No grade band matches the supplied percentage."));

        return GradeBandResponse.from(gradeBand);
    }

    @Transactional
    public GradingScaleResponse createGradingScale(CreateGradingScaleRequest request) {
        String name = request.name().trim();
        if (gradingScales.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A grading scale with that name already exists.");
        }

        var bands = request.bands().stream()
            .sorted(Comparator.comparing(CreateGradeBandRequest::minimumPercentage))
            .toList();
        for (CreateGradeBandRequest band : bands) {
            if (band.minimumPercentage().compareTo(band.maximumPercentage()) > 0) {
                throw new IllegalArgumentException("Grade band minimum percentage cannot exceed its maximum percentage.");
            }
        }
        for (int index = 1; index < bands.size(); index++) {
            CreateGradeBandRequest previous = bands.get(index - 1);
            CreateGradeBandRequest current = bands.get(index);
            if (current.minimumPercentage().compareTo(previous.maximumPercentage()) <= 0) {
                throw new IllegalArgumentException("Grade bands must not overlap.");
            }
        }

        GradingScale gradingScale = gradingScales.save(new GradingScale(name, bands));
        return GradingScaleResponse.from(gradingScale);
    }
}
