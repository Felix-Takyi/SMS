package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class TimetableController {
    private final TimetableService timetables;

    public TimetableController(TimetableService timetables) {
        this.timetables = timetables;
    }

    @GetMapping("/teachers/available")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    public List<TeacherOptionResponse> listAvailableTeachers() {
        return timetables.listTeachers();
    }

    @GetMapping("/teacher-subject-assignments")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public List<TeacherSubjectAssignmentResponse> listAssignments(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID schoolClassId) {
        return timetables.listAssignments(academicYearId, schoolClassId);
    }

    @PostMapping("/teacher-subject-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    public TeacherSubjectAssignmentResponse createAssignment(
            @Valid @RequestBody CreateTeacherSubjectAssignmentRequest request) {
        return timetables.createAssignment(request);
    }

    @DeleteMapping("/teacher-subject-assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    public void deleteAssignment(@PathVariable UUID assignmentId) {
        timetables.deleteAssignment(assignmentId);
    }

    @GetMapping("/timetable-entries")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public List<TimetableEntryResponse> listTimetable(
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) UUID schoolClassId) {
        return timetables.listTimetable(academicYearId, schoolClassId);
    }

    @PostMapping("/timetable-entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    public TimetableEntryResponse createTimetableEntry(
            @Valid @RequestBody CreateTimetableEntryRequest request) {
        return timetables.createTimetableEntry(request);
    }

    @PutMapping("/timetable-entries/{entryId}")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    public TimetableEntryResponse updateTimetableEntry(@PathVariable UUID entryId,
            @Valid @RequestBody CreateTimetableEntryRequest request) {
        return timetables.updateTimetableEntry(entryId, request);
    }

    @DeleteMapping("/timetable-entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    public void deleteTimetableEntry(@PathVariable UUID entryId) {
        timetables.deleteTimetableEntry(entryId);
    }

    @GetMapping("/timetable.pdf")
    @PreAuthorize("hasAuthority('ACADEMICS_VIEW')")
    public ResponseEntity<byte[]> downloadTimetable(
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) UUID schoolClassId,
            @RequestParam(defaultValue = "false") boolean inline) {
        byte[] pdf = timetables.renderTimetablePdf(academicYearId, schoolClassId);
        String filename = "timetable-" + academicYearId + (schoolClassId == null ? "" : "-" + schoolClassId) + ".pdf";
        ContentDisposition disposition = inline
            ? ContentDisposition.inline().filename(filename).build()
            : ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(pdf);
    }
}