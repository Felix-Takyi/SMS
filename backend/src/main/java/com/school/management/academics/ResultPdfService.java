package com.school.management.academics;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.management.audit.AuditLog;
import com.school.management.audit.AuditLogRepository;
import com.school.management.students.Student;
import com.school.management.students.StudentRepository;
import com.school.management.users.AppUser;
import com.school.management.users.Role;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ResultPdfService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final StudentRepository students;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final TermRepository terms;
    private final AssessmentRepository assessments;
    private final AssessmentScoreRepository assessmentScores;
    private final GradingScaleRepository gradingScales;
    private final AuditLogRepository auditLogs;

    public ResultPdfService(StudentRepository students,
                           AcademicYearRepository academicYears,
                           SchoolClassRepository schoolClasses,
                           TermRepository terms,
                           AssessmentRepository assessments,
                           AssessmentScoreRepository assessmentScores,
                           GradingScaleRepository gradingScales,
                           AuditLogRepository auditLogs) {
        this.students = students;
        this.academicYears = academicYears;
        this.schoolClasses = schoolClasses;
        this.terms = terms;
        this.assessments = assessments;
        this.assessmentScores = assessmentScores;
        this.gradingScales = gradingScales;
        this.auditLogs = auditLogs;
    }

    @Transactional
    public byte[] generateStudentResultPdf(UUID studentId,
                                          UUID academicYearId,
                                          UUID classId,
                                          UUID termId,
                                          UUID subjectId,
                                          AppUser actor,
                                          HttpServletRequest request) {
        ensureAuthorizedUser();

        Student student = students.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));
        AcademicYear academicYear = academicYears.findById(academicYearId)
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        SchoolClass schoolClass = schoolClasses.findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("Class was not found."));
        Term term = termId == null ? null : terms.findById(termId)
            .orElseThrow(() -> new IllegalArgumentException("Term was not found."));
        validateTermYear(term, academicYearId);

        List<Assessment> classAssessments = findClassAssessments(academicYearId, classId, term, subjectId, true);

        if (classAssessments.isEmpty()) {
            throw new IllegalArgumentException("No results are available for the selected academic year and class.");
        }

        List<AssessmentScore> studentScores = assessmentScores.findAll().stream()
            .filter(score -> score.getStudent().getId().equals(studentId))
            .filter(score -> classAssessments.stream().anyMatch(assessment -> assessment.getId().equals(score.getAssessment().getId())))
            .toList();

        if (studentScores.isEmpty()) {
            throw new IllegalArgumentException("No result scores are available for this student in the selected class.");
        }

        GradingScale gradingScale = gradingScales.findAll().stream()
            .sorted(Comparator.comparing(GradingScale::getName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No grading scale configured."));

        StudentResultSummary summary = buildStudentResultSummary(classAssessments, studentScores, gradingScale);
        byte[] pdf = renderStudentResultPdf(student, academicYear, schoolClass, term, summary);

        auditLogs.save(new AuditLog(
            actor.getId(),
            "RESULT_PDF_GENERATED",
            "RESULTS",
            student.getId().toString(),
            Instant.now(),
            request.getRemoteAddr(),
            request.getHeader("User-Agent")
        ));

        return pdf;
    }

    @Transactional
    public byte[] generateClassResultPdf(UUID classId,
                                         UUID academicYearId,
                                         UUID termId,
                                         UUID subjectId,
                                         AppUser actor,
                                         HttpServletRequest request) {
        ensureAuthorizedUser();

        SchoolClass schoolClass = schoolClasses.findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("Class was not found."));
        AcademicYear academicYear = academicYears.findById(academicYearId)
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        Term term = termId == null ? null : terms.findById(termId)
            .orElseThrow(() -> new IllegalArgumentException("Term was not found."));
        validateTermYear(term, academicYearId);

        List<Assessment> classAssessments = findClassAssessments(academicYearId, classId, term, subjectId, false);

        if (classAssessments.isEmpty()) {
            throw new IllegalArgumentException("No class results are available for the selected academic year and class.");
        }

        GradingScale gradingScale = gradingScales.findAll().stream()
            .sorted(Comparator.comparing(GradingScale::getName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No grading scale configured."));

        List<ClassResultRow> classRows = students.findAll().stream()
            .filter(student -> !student.getStatus().equalsIgnoreCase("ARCHIVED"))
            .map(student -> {
                List<AssessmentScore> scores = assessmentScores.findAll().stream()
                    .filter(score -> score.getStudent().getId().equals(student.getId()))
                    .filter(score -> classAssessments.stream().anyMatch(assessment -> assessment.getId().equals(score.getAssessment().getId())))
                    .toList();
                StudentResultSummary summary = buildStudentResultSummary(classAssessments, scores, gradingScale);
                return new ClassResultRow(student, summary);
            })
            .filter(row -> row.summary() != null && !row.summary().subjectRows().isEmpty())
            .toList();

        if (classRows.isEmpty()) {
            throw new IllegalArgumentException("No results are available for the selected class.");
        }

        byte[] pdf = renderClassResultPdf(schoolClass, academicYear, term, classRows);

        auditLogs.save(new AuditLog(
            actor.getId(),
            "RESULT_PDF_GENERATED",
            "RESULTS",
            schoolClass.getId().toString(),
            Instant.now(),
            request.getRemoteAddr(),
            request.getHeader("User-Agent")
        ));

        return pdf;
    }

    public static StudentResultSummary buildStudentResultSummary(List<Assessment> assessments,
                                                                List<AssessmentScore> scores,
                                                                GradingScale gradingScale) {
        Map<UUID, BigDecimal> scoreByAssessment = scores.stream()
            .collect(Collectors.toMap(
                score -> score.getAssessment().getId(),
                AssessmentScore::getScore,
                BigDecimal::add,
                LinkedHashMap::new
            ));

        Map<UUID, List<Assessment>> assessmentsBySubject = assessments.stream()
            .collect(Collectors.groupingBy(assessment -> assessment.getSubject().getId(), LinkedHashMap::new, Collectors.toList()));

        List<SubjectResultRow> subjectRows = new ArrayList<>();
        List<AssessmentDetailRow> detailRows = new ArrayList<>();

        List<UUID> subjectIds = assessmentsBySubject.keySet().stream()
            .sorted(Comparator.comparing(uuid -> assessmentsBySubject.get(uuid).getFirst().getSubject().getName()))
            .toList();

        for (UUID subjectId : subjectIds) {
            List<Assessment> subjectAssessments = assessmentsBySubject.get(subjectId).stream()
                .sorted(Comparator.comparing(Assessment::getAssessmentDate))
                .toList();

            BigDecimal subjectScore = BigDecimal.ZERO;
            BigDecimal subjectTotal = BigDecimal.ZERO;
            for (Assessment assessment : subjectAssessments) {
                BigDecimal score = scoreByAssessment.getOrDefault(assessment.getId(), BigDecimal.ZERO);
                subjectScore = subjectScore.add(score);
                subjectTotal = subjectTotal.add(assessment.getTotalMarks());

                BigDecimal percentage = calculatePercentage(score, assessment.getTotalMarks());
                GradeBand band = resolveBand(gradingScale, percentage);
                detailRows.add(new AssessmentDetailRow(
                    assessment.getSubject().getCode(),
                    assessment.getSubject().getName(),
                    assessment.getAssessmentType(),
                    score,
                    assessment.getTotalMarks(),
                    percentage,
                    band == null ? "N/A" : band.getGradeLabel(),
                    band == null ? "" : band.getRemark() == null ? "" : band.getRemark()
                ));
            }

            BigDecimal subjectPercentage = calculatePercentage(subjectScore, subjectTotal);
            GradeBand subjectBand = resolveBand(gradingScale, subjectPercentage);
            subjectRows.add(new SubjectResultRow(
                subjectAssessments.getFirst().getSubject().getCode(),
                subjectAssessments.getFirst().getSubject().getName(),
                subjectScore,
                subjectTotal,
                subjectPercentage,
                subjectBand == null ? "N/A" : subjectBand.getGradeLabel(),
                subjectBand == null ? "" : subjectBand.getRemark() == null ? "" : subjectBand.getRemark()
            ));
        }

        BigDecimal average = subjectRows.isEmpty() ? BigDecimal.ZERO : subjectRows.stream()
            .map(SubjectResultRow::percentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(subjectRows.size()), 2, RoundingMode.HALF_UP);

        GradeBand averageBand = resolveBand(gradingScale, average);
        return new StudentResultSummary(subjectRows, detailRows, average, averageBand == null ? "N/A" : averageBand.getGradeLabel());
    }

    private static BigDecimal calculatePercentage(BigDecimal earned, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return earned.divide(total, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    private static GradeBand resolveBand(GradingScale gradingScale, BigDecimal percentage) {
        if (gradingScale == null || gradingScale.getBands().isEmpty()) {
            return null;
        }
        return gradingScale.getBands().stream()
            .filter(band -> percentage.compareTo(band.getMinimumPercentage()) >= 0
                && percentage.compareTo(band.getMaximumPercentage()) <= 0)
            .findFirst()
            .orElse(null);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private byte[] renderStudentResultPdf(Student student,
                                         AcademicYear academicYear,
                                         SchoolClass schoolClass,
                                         Term term,
                                         StudentResultSummary summary) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("Official Student Result Report", titleFont));
            document.add(new Paragraph("School Management System", new Font(Font.HELVETICA, 11, Font.NORMAL)));
            document.add(new Paragraph("Academic Year: " + academicYear.getName() + (term == null ? "" : " | Term: " + term.getName()), valueFont));
            document.add(new Paragraph("Generated: " + Instant.now(), valueFont));
            document.add(new Paragraph(" ", valueFont));

            PdfPTable studentTable = new PdfPTable(2);
            studentTable.setWidthPercentage(100);
            studentTable.setSpacingAfter(12f);
            studentTable.addCell(createCell("Student", labelFont));
            studentTable.addCell(createCell(fullName(student), valueFont));
            studentTable.addCell(createCell("Admission Number", labelFont));
            studentTable.addCell(createCell(student.getAdmissionNumber(), valueFont));
            studentTable.addCell(createCell("Class", labelFont));
            studentTable.addCell(createCell(schoolClass.getName(), valueFont));
            studentTable.addCell(createCell("Gender", labelFont));
            studentTable.addCell(createCell(student.getGender(), valueFont));
            document.add(studentTable);

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(12f);
            summaryTable.addCell(createCell("Subjects", labelFont));
            summaryTable.addCell(createCell(String.valueOf(summary.subjectRows().size()), valueFont));
            summaryTable.addCell(createCell("Average", labelFont));
            summaryTable.addCell(createCell(summary.averagePercentage().setScale(2, RoundingMode.HALF_UP) + "%", valueFont));
            summaryTable.addCell(createCell("Overall Grade", labelFont));
            summaryTable.addCell(createCell(summary.overallGrade(), valueFont));
            summaryTable.addCell(createCell("Report Date", labelFont));
            summaryTable.addCell(createCell(formatDate(LocalDate.now()), valueFont));
            document.add(summaryTable);

            PdfPTable detailTable = new PdfPTable(7);
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingBefore(6f);
            detailTable.setSpacingAfter(12f);
            detailTable.addCell(createHeaderCell("Subject", labelFont));
            detailTable.addCell(createHeaderCell("Assessment", labelFont));
            detailTable.addCell(createHeaderCell("Score", labelFont));
            detailTable.addCell(createHeaderCell("Max", labelFont));
            detailTable.addCell(createHeaderCell("%", labelFont));
            detailTable.addCell(createHeaderCell("Grade", labelFont));
            detailTable.addCell(createHeaderCell("Remark", labelFont));

            for (AssessmentDetailRow row : summary.assessmentRows()) {
                detailTable.addCell(createCell(row.subjectName(), valueFont));
                detailTable.addCell(createCell(row.assessmentType(), valueFont));
                detailTable.addCell(createCell(row.score().setScale(2, RoundingMode.HALF_UP).toPlainString(), valueFont));
                detailTable.addCell(createCell(row.maxScore().setScale(2, RoundingMode.HALF_UP).toPlainString(), valueFont));
                detailTable.addCell(createCell(row.percentage().setScale(2, RoundingMode.HALF_UP).toPlainString() + "%", valueFont));
                detailTable.addCell(createCell(row.grade(), valueFont));
                detailTable.addCell(createCell(row.remark(), valueFont));
            }
            document.add(detailTable);

            document.add(new Paragraph("Class Teacher: ____________________________", valueFont));
            document.add(new Paragraph("Head Teacher/Headmaster: ____________________________", valueFont));
            document.add(new Paragraph("Date: ____________________________", valueFont));

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF generation failed for the student result report.", e);
        }
        return outputStream.toByteArray();
    }

    private byte[] renderClassResultPdf(SchoolClass schoolClass,
                                       AcademicYear academicYear,
                                       Term term,
                                       List<ClassResultRow> classRows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 40, 40);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            document.add(new Paragraph("Class Result Summary", titleFont));
            document.add(new Paragraph("School: School Management System", valueFont));
            document.add(new Paragraph("Academic Year: " + academicYear.getName() + (term == null ? "" : " | Term: " + term.getName()), valueFont));
            document.add(new Paragraph("Class: " + schoolClass.getName() + " | Generated: " + Instant.now(), valueFont));
            document.add(new Paragraph(" ", valueFont));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            table.addCell(createHeaderCell("Admission No.", labelFont));
            table.addCell(createHeaderCell("Student", labelFont));
            table.addCell(createHeaderCell("Subjects", labelFont));
            table.addCell(createHeaderCell("Average", labelFont));
            table.addCell(createHeaderCell("Grade", labelFont));
            table.addCell(createHeaderCell("Remark", labelFont));

            for (ClassResultRow classRow : classRows) {
                Student student = classRow.student();
                StudentResultSummary studentSummary = classRow.summary();
                table.addCell(createCell(student.getAdmissionNumber(), valueFont));
                table.addCell(createCell(fullName(student), valueFont));
                table.addCell(createCell(String.valueOf(studentSummary.subjectRows().size()), valueFont));
                table.addCell(createCell(studentSummary.averagePercentage().setScale(2, RoundingMode.HALF_UP).toPlainString() + "%", valueFont));
                table.addCell(createCell(studentSummary.overallGrade(), valueFont));
                table.addCell(createCell(studentSummary.overallGrade(), valueFont));
            }
            document.add(table);
            document.close();
        } catch (DocumentException exception) {
            throw new IllegalStateException("PDF generation failed for the class result report.", exception);
        }
        return outputStream.toByteArray();
    }

    private void ensureAuthorizedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasResultPdfAccess(authentication)) {
            throw new AccessDeniedException("You do not have permission to generate official result PDFs.");
        }
    }

    public boolean hasResultPdfAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean hasResultPermission = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals("RESULT_VIEW") || authority.equals("REPORT_VIEW"));

        boolean hasAdminRole = authentication.getPrincipal() instanceof AppUser user
            && user.getRoles().stream().map(Role::getCode).anyMatch(code -> code.equals("SUPER_ADMIN") || code.equals("ADMIN"));

        return hasResultPermission && hasAdminRole;
    }

    private List<Assessment> findClassAssessments(UUID academicYearId,
                                                 UUID classId,
                                                 Term term,
                                                 UUID subjectId,
                                                 boolean sort) {
        var matchingAssessments = assessments.findAll().stream()
            .filter(assessment -> assessment.getAcademicYear().getId().equals(academicYearId))
            .filter(assessment -> assessment.getSchoolClass().getId().equals(classId))
            .filter(assessment -> subjectId == null || assessment.getSubject().getId().equals(subjectId))
            .filter(assessment -> term == null || (!assessment.getAssessmentDate().isBefore(term.getStartDate())
                && !assessment.getAssessmentDate().isAfter(term.getEndDate())));
        return (sort
            ? matchingAssessments.sorted(Comparator.comparing((Assessment assessment) -> assessment.getSubject().getName())
                .thenComparing(Assessment::getAssessmentDate))
            : matchingAssessments).toList();
    }

    private void validateTermYear(Term term, UUID academicYearId) {
        if (term != null && !term.getAcademicYear().getId().equals(academicYearId)) {
            throw new IllegalArgumentException("The selected term does not belong to the selected academic year.");
        }
    }

    public String buildStudentFileName(String studentName, String className, String termName) {
        String cleanedStudent = sanitize(studentName);
        String cleanedClass = sanitize(className);
        String cleanedTerm = sanitize(termName);
        return String.join("_",
            cleanedStudent.isBlank() ? "student" : cleanedStudent,
            cleanedClass.isBlank() ? "result" : cleanedClass,
            cleanedTerm.isBlank() ? "term" : cleanedTerm,
            "Result.pdf"
        );
    }

    public String buildClassFileName(String className, String termName) {
        String cleanedClass = sanitize(className);
        String cleanedTerm = sanitize(termName);
        return String.join("_",
            cleanedClass.isBlank() ? "class" : cleanedClass,
            cleanedTerm.isBlank() ? "term" : cleanedTerm,
            "Class_Results.pdf"
        );
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
    }

    private static String fullName(Student student) {
        return String.join(" ",
            student.getFirstName(),
            student.getMiddleName() == null || student.getMiddleName().isBlank() ? "" : student.getMiddleName(),
            student.getLastName()
        ).replaceAll("\\s+", " ").trim();
    }

    private PdfPCell createCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setBorderWidth(0.5f);
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell createHeaderCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setBorderWidth(0.8f);
        cell.setPadding(5f);
        cell.setBackgroundColor(new Color(230, 238, 242));
        return cell;
    }

    public record StudentResultSummary(List<SubjectResultRow> subjectRows,
                                      List<AssessmentDetailRow> assessmentRows,
                                      BigDecimal averagePercentage,
                                      String overallGrade) {
    }

    public record ClassResultRow(Student student, StudentResultSummary summary) {
    }

    public record SubjectResultRow(String subjectCode,
                                   String subjectName,
                                   BigDecimal totalScore,
                                   BigDecimal totalMarks,
                                   BigDecimal percentage,
                                   String grade,
                                   String remark) {
    }

    public record AssessmentDetailRow(String subjectCode,
                                     String subjectName,
                                     String assessmentType,
                                     BigDecimal score,
                                     BigDecimal maxScore,
                                     BigDecimal percentage,
                                     String grade,
                                     String remark) {
    }
}
