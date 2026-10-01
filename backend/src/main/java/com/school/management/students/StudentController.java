package com.school.management.students;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students")
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public Page<StudentResponse> listStudents(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return studentService.listStudents(PageRequest.of(page, size, Sort.by("admissionNumber").ascending()));
    }

    @GetMapping("/students/{id}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public StudentResponse getStudent(@PathVariable UUID id) {
        return studentService.getStudent(id);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.createStudent(request);
    }

    @PutMapping("/students/{id}")
    @PreAuthorize("hasAuthority('STUDENT_EDIT')")
    public StudentResponse updateStudent(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.updateStudent(id, request);
    }
}
