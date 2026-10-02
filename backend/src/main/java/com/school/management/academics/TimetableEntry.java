package com.school.management.academics;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_subject_assignment_id", nullable = false)
    private TeacherSubjectAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 80)
    private String room;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TimetableEntry() {
    }

    public TimetableEntry(TeacherSubjectAssignment assignment, DayOfWeek dayOfWeek,
                          LocalTime startTime, LocalTime endTime, String room) {
        this.assignment = assignment;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room.trim();
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(TeacherSubjectAssignment assignment, DayOfWeek dayOfWeek,
                       LocalTime startTime, LocalTime endTime, String room) {
        this.assignment = assignment;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room.trim();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public TeacherSubjectAssignment getAssignment() { return assignment; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getRoom() { return room; }
}