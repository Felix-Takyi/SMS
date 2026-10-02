package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.school.management.students.Student;

class ResultPdfServiceTest {
    @Test
    void buildStudentResultSummaryCalculatesAverageAndGrade() {
        Student student = new Student(
            "ADM-001",
            "Felix",
            "",
            "Takyi",
            LocalDate.of(2014, 5, 12),
            "MALE",
            "Ghanaian",
            "Christian",
            "0240000000",
            "felix@example.com",
            "Accra",
            "ACTIVE"
        );

        Subject mathematics = new Subject("MATH", "Mathematics", "Core subject", true);
        Subject english = new Subject("ENG", "English", "Language subject", true);
        ReflectionTestUtils.setField(mathematics, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(english, "id", UUID.randomUUID());

        AcademicYear academicYear = new AcademicYear(
            "2026/2027",
            "2026/2027 Academic Year",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            true
        );
        SchoolClass schoolClass = new SchoolClass("BASIC6", "Basic 6", true);

        Assessment quiz = new Assessment(mathematics, academicYear, schoolClass, "QUIZ",
            new BigDecimal("20"), LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7));
        Assessment test = new Assessment(mathematics, academicYear, schoolClass, "TEST",
            new BigDecimal("80"), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5));
        Assessment essay = new Assessment(english, academicYear, schoolClass, "ESSAY",
            new BigDecimal("100"), LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 8));
        ReflectionTestUtils.setField(quiz, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(test, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(essay, "id", UUID.randomUUID());

        GradingScale gradingScale = new GradingScale("Default", List.of(
            new CreateGradeBandRequest("A", new BigDecimal("80"), new BigDecimal("100"), "Excellent"),
            new CreateGradeBandRequest("B", new BigDecimal("70"), new BigDecimal("79.99"), "Very Good"),
            new CreateGradeBandRequest("C", new BigDecimal("60"), new BigDecimal("69.99"), "Good"),
            new CreateGradeBandRequest("D", new BigDecimal("50"), new BigDecimal("59.99"), "Satisfactory"),
            new CreateGradeBandRequest("E", new BigDecimal("0"), new BigDecimal("49.99"), "Needs support")
        ));

        List<AssessmentScore> scores = List.of(
            new AssessmentScore(quiz, student, new BigDecimal("18")),
            new AssessmentScore(test, student, new BigDecimal("72")),
            new AssessmentScore(essay, student, new BigDecimal("88"))
        );

        ResultPdfService.StudentResultSummary summary = ResultPdfService.buildStudentResultSummary(
            List.of(quiz, test, essay), scores, gradingScale
        );

        assertEquals(2, summary.subjectRows().size());
        assertEquals(new BigDecimal("89.00"), summary.averagePercentage());
        assertEquals("A", summary.overallGrade());
    }
}
