package com.school.management.academics;

import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.school.management.users.AppUser;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
public class ResultPdfController {
    private final ResultPdfService resultPdfService;

    public ResultPdfController(ResultPdfService resultPdfService) {
        this.resultPdfService = resultPdfService;
    }

    @GetMapping("/results/students/{studentId}/pdf")
    @PreAuthorize("@resultPdfService.hasResultPdfAccess(authentication)")
    public ResponseEntity<byte[]> generateStudentResultPdf(@PathVariable UUID studentId,
                                                         @RequestParam UUID academicYearId,
                                                         @RequestParam UUID classId,
                                                         @RequestParam(required = false) UUID termId,
                                                         @RequestParam(required = false) UUID subjectId,
                                                         @RequestParam(defaultValue = "false") boolean inline,
                                                         @AuthenticationPrincipal AppUser currentUser,
                                                         HttpServletRequest request) {
        byte[] pdf = resultPdfService.generateStudentResultPdf(studentId, academicYearId, classId, termId, subjectId, currentUser, request);
        String filename = buildFilename(studentId, classId, termId, "student");
        ContentDisposition disposition = inline
            ? ContentDisposition.inline().filename(filename).build()
            : ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(pdf);
    }

    @GetMapping("/results/classes/{classId}/pdf")
    @PreAuthorize("@resultPdfService.hasResultPdfAccess(authentication)")
    public ResponseEntity<byte[]> generateClassResultPdf(@PathVariable UUID classId,
                                                       @RequestParam UUID academicYearId,
                                                       @RequestParam(required = false) UUID termId,
                                                       @RequestParam(required = false) UUID subjectId,
                                                       @RequestParam(defaultValue = "false") boolean inline,
                                                       @AuthenticationPrincipal AppUser currentUser,
                                                       HttpServletRequest request) {
        byte[] pdf = resultPdfService.generateClassResultPdf(classId, academicYearId, termId, subjectId, currentUser, request);
        String filename = buildFilename(classId, termId, null, "class");
        ContentDisposition disposition = inline
            ? ContentDisposition.inline().filename(filename).build()
            : ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(pdf);
    }

    private String buildFilename(UUID id, UUID classId, UUID termId, String type) {
        StringBuilder name = new StringBuilder();
        if ("student".equals(type)) {
            name.append("Student_").append(id.toString().substring(0, 8));
        } else {
            name.append("Class_").append(classId == null ? id.toString().substring(0, 8) : classId.toString().substring(0, 8));
        }
        if (termId != null) {
            name.append("_Term").append(termId.toString().substring(0, 8));
        }
        name.append("_Result.pdf");
        return name.toString();
    }
}
