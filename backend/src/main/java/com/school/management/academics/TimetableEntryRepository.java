package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    @Query("select entry from TimetableEntry entry "
        + "join fetch entry.assignment assignment join fetch assignment.academicYear "
        + "join fetch assignment.schoolClass join fetch assignment.subject join fetch assignment.teacher")
    List<TimetableEntry> findAllWithRelations();
}