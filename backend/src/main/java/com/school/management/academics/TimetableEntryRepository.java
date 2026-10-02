package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    @EntityGraph(attributePaths = {"assignment", "assignment.academicYear", "assignment.schoolClass", "assignment.subject", "assignment.teacher"})
    List<TimetableEntry> findAll();
}