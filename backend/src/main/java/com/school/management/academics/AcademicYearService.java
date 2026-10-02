package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicYearService {
    private final AcademicYearRepository academicYears;
    private final TermRepository terms;

    public AcademicYearService(AcademicYearRepository academicYears, TermRepository terms) {
        this.academicYears = academicYears;
        this.terms = terms;
    }

    @Transactional(readOnly = true)
    public Page<AcademicYearResponse> listAcademicYears(Pageable pageable) {
        return academicYears.findAll(pageable).map(AcademicYearResponse::from);
    }

    @Transactional
    public AcademicYearResponse createAcademicYear(CreateAcademicYearRequest request) {
        String code = request.code().trim();
        if (academicYears.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("An academic year with that code already exists.");
        }

        AcademicYear academicYear = new AcademicYear(
            code,
            request.name(),
            request.startDate(),
            request.endDate(),
            request.active()
        );
        return AcademicYearResponse.from(academicYears.save(academicYear));
    }

    @Transactional
    public AcademicYearResponse updateAcademicYear(UUID id, CreateAcademicYearRequest request) {
        AcademicYear academicYear = academicYears.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        String code = request.code().trim();
        if (academicYears.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("An academic year with that code already exists.");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Academic year end date must not be before its start date.");
        }
        academicYear.update(code, request.name(), request.startDate(), request.endDate(), request.active());
        return AcademicYearResponse.from(academicYear);
    }

    @Transactional(readOnly = true)
    public List<TermResponse> listTermsForAcademicYear(String academicYearCode) {
        AcademicYear academicYear = academicYears.findByCodeIgnoreCase(academicYearCode)
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        return terms.findByAcademicYearIdOrderByStartDateAsc(academicYear.getId())
            .stream().map(TermResponse::from).toList();
    }

    @Transactional
    public TermResponse createTerm(CreateTermRequest request) {
        AcademicYear academicYear = academicYears.findByCodeIgnoreCase(request.academicYearCode())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));

        Term term = new Term(
            academicYear,
            request.name(),
            request.startDate(),
            request.endDate(),
            true
        );
        return TermResponse.from(terms.save(term));
    }

    @Transactional
    public TermResponse updateTerm(UUID id, UpdateTermRequest request) {
        Term term = terms.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Term was not found."));
        AcademicYear academicYear = academicYears.findByCodeIgnoreCase(request.academicYearCode())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Term end date must not be before its start date.");
        }
        term.update(academicYear, request.name(), request.startDate(), request.endDate(), request.active());
        return TermResponse.from(term);
    }
}
