package com.school.management.academics;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;
import com.school.management.users.Role;

@Service
public class TimetableService {
    private final TeacherSubjectAssignmentRepository assignments;
    private final TimetableEntryRepository entries;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final SubjectRepository subjects;
    private final AppUserRepository users;

    public TimetableService(TeacherSubjectAssignmentRepository assignments,
                            TimetableEntryRepository entries,
                            AcademicYearRepository academicYears,
                            SchoolClassRepository schoolClasses,
                            SubjectRepository subjects,
                            AppUserRepository users) {
        this.assignments = assignments;
        this.entries = entries;
        this.academicYears = academicYears;
        this.schoolClasses = schoolClasses;
        this.subjects = subjects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<TeacherOptionResponse> listTeachers() {
        return users.findDistinctByEnabledTrueAndRoles_CodeOrderByDisplayNameAsc("TEACHER")
            .stream().map(TeacherOptionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherSubjectAssignmentResponse> listAssignments(UUID academicYearId, UUID schoolClassId) {
        return assignments.findAllWithRelations().stream()
            .filter(assignment -> academicYearId == null || assignment.getAcademicYear().getId().equals(academicYearId))
            .filter(assignment -> schoolClassId == null || assignment.getSchoolClass().getId().equals(schoolClassId))
            .sorted(Comparator.comparing((TeacherSubjectAssignment assignment) -> assignment.getSchoolClass().getName())
                .thenComparing(assignment -> assignment.getSubject().getName()))
            .map(TeacherSubjectAssignmentResponse::from)
            .toList();
    }

    @Transactional
    public TeacherSubjectAssignmentResponse createAssignment(CreateTeacherSubjectAssignmentRequest request) {
        AcademicYear academicYear = academicYears.findById(request.academicYearId())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        SchoolClass schoolClass = schoolClasses.findById(request.schoolClassId())
            .orElseThrow(() -> new IllegalArgumentException("Class was not found."));
        Subject subject = subjects.findById(request.subjectId())
            .orElseThrow(() -> new IllegalArgumentException("Subject was not found."));
        AppUser teacher = users.findById(request.teacherUserId())
            .orElseThrow(() -> new IllegalArgumentException("Teacher account was not found."));

        if (!teacher.isEnabled() || teacher.getRoles().stream().map(Role::getCode).noneMatch("TEACHER"::equals)) {
            throw new IllegalArgumentException("Choose an enabled user account with the TEACHER role.");
        }
        if (assignments.existsByAcademicYearIdAndSchoolClassIdAndSubjectId(
                request.academicYearId(), request.schoolClassId(), request.subjectId())) {
            throw new IllegalArgumentException("A teacher is already assigned to this class, subject and academic year.");
        }

        return TeacherSubjectAssignmentResponse.from(assignments.save(
            new TeacherSubjectAssignment(academicYear, schoolClass, subject, teacher)));
    }

    @Transactional
    public void deleteAssignment(UUID assignmentId) {
        if (!assignments.existsById(assignmentId)) {
            throw new IllegalArgumentException("Teacher assignment was not found.");
        }
        if (entries.findAllWithRelations().stream().anyMatch(entry -> entry.getAssignment().getId().equals(assignmentId))) {
            throw new IllegalArgumentException("Remove this assignment's timetable entries before deleting it.");
        }
        assignments.deleteById(assignmentId);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> listTimetable(UUID academicYearId, UUID schoolClassId) {
        if (!academicYears.existsById(academicYearId)) {
            throw new IllegalArgumentException("Academic year was not found.");
        }
        if (schoolClassId != null && !schoolClasses.existsById(schoolClassId)) {
            throw new IllegalArgumentException("Class was not found.");
        }
        return entries.findAllWithRelations().stream()
            .filter(entry -> entry.getAssignment().getAcademicYear().getId().equals(academicYearId))
            .filter(entry -> schoolClassId == null || entry.getAssignment().getSchoolClass().getId().equals(schoolClassId))
            .sorted(Comparator.comparingInt((TimetableEntry entry) -> entry.getDayOfWeek().getValue())
                .thenComparing(TimetableEntry::getStartTime)
                .thenComparing(entry -> entry.getAssignment().getSchoolClass().getName()))
            .map(TimetableEntryResponse::from)
            .toList();
    }

    @Transactional
    public TimetableEntryResponse createTimetableEntry(CreateTimetableEntryRequest request) {
        TeacherSubjectAssignment assignment = getAssignment(request.assignmentId());
        validateTimes(request.startTime(), request.endTime());
        ensureNoConflict(null, assignment, request.dayOfWeek(), request.startTime(), request.endTime(), request.room());
        return TimetableEntryResponse.from(entries.save(new TimetableEntry(assignment, request.dayOfWeek(),
            request.startTime(), request.endTime(), request.room())));
    }

    @Transactional
    public TimetableEntryResponse updateTimetableEntry(UUID entryId, CreateTimetableEntryRequest request) {
        TimetableEntry entry = entries.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Timetable entry was not found."));
        TeacherSubjectAssignment assignment = getAssignment(request.assignmentId());
        validateTimes(request.startTime(), request.endTime());
        ensureNoConflict(entryId, assignment, request.dayOfWeek(), request.startTime(), request.endTime(), request.room());
        entry.update(assignment, request.dayOfWeek(), request.startTime(), request.endTime(), request.room());
        return TimetableEntryResponse.from(entry);
    }

    @Transactional
    public void deleteTimetableEntry(UUID entryId) {
        if (!entries.existsById(entryId)) {
            throw new IllegalArgumentException("Timetable entry was not found.");
        }
        entries.deleteById(entryId);
    }

    @Transactional(readOnly = true)
    public byte[] renderTimetablePdf(UUID academicYearId, UUID schoolClassId) {
        List<TimetableEntryResponse> schedule = listTimetable(academicYearId, schoolClassId);
        AcademicYear academicYear = academicYears.findById(academicYearId)
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));
        String className = schoolClassId == null ? "All classes" : schoolClasses.findById(schoolClassId)
            .orElseThrow(() -> new IllegalArgumentException("Class was not found.")).getName();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 32, 32, 36, 36);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("School Timetable", new Font(Font.HELVETICA, 18, Font.BOLD)));
            document.add(new Paragraph("Academic year: " + academicYear.getName() + " | " + className,
                new Font(Font.HELVETICA, 10, Font.NORMAL)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[] {1.1f, 1.4f, 1.5f, 2.2f, 2.2f, 1.3f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String heading : List.of("DAY", "TIME", "CLASS", "SUBJECT", "TEACHER", "ROOM")) {
                PdfPCell cell = new PdfPCell(new Phrase(heading, new Font(Font.HELVETICA, 9, Font.BOLD)));
                cell.setPadding(6f);
                table.addCell(cell);
            }
            for (TimetableEntryResponse row : schedule) {
                table.addCell(pdfCell(row.dayOfWeek().toString()));
                table.addCell(pdfCell(row.startTime() + " - " + row.endTime()));
                table.addCell(pdfCell(row.schoolClassName()));
                table.addCell(pdfCell(row.subjectName()));
                table.addCell(pdfCell(row.teacherName()));
                table.addCell(pdfCell(row.room()));
            }
            if (schedule.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No timetable entries are configured."));
                empty.setColspan(6);
                empty.setPadding(8f);
                table.addCell(empty);
            }
            document.add(table);
            document.close();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Could not generate the timetable PDF.", exception);
        }
        return output.toByteArray();
    }

    private TeacherSubjectAssignment getAssignment(UUID assignmentId) {
        return assignments.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher assignment was not found."));
    }

    private void ensureNoConflict(UUID ignoredEntryId, TeacherSubjectAssignment assignment, DayOfWeek day,
                                  LocalTime start, LocalTime end, String room) {
        boolean conflict = entries.findAllWithRelations().stream()
            .filter(existing -> !existing.getId().equals(ignoredEntryId))
            .filter(existing -> existing.getDayOfWeek() == day)
            .filter(existing -> existing.getAssignment().getAcademicYear().getId().equals(assignment.getAcademicYear().getId()))
            .filter(existing -> overlaps(start, end, existing.getStartTime(), existing.getEndTime()))
            .anyMatch(existing -> existing.getAssignment().getSchoolClass().getId().equals(assignment.getSchoolClass().getId())
                || existing.getAssignment().getTeacher().getId().equals(assignment.getTeacher().getId())
                || existing.getRoom().equalsIgnoreCase(room.trim()));
        if (conflict) {
            throw new IllegalArgumentException("This time overlaps an existing class, teacher or room timetable entry.");
        }
    }

    static boolean overlaps(LocalTime start, LocalTime end, LocalTime existingStart, LocalTime existingEnd) {
        return start.isBefore(existingEnd) && end.isAfter(existingStart);
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("The timetable end time must be after its start time.");
        }
    }

    private PdfPCell pdfCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, new Font(Font.HELVETICA, 8)));
        cell.setPadding(5f);
        return cell;
    }
}