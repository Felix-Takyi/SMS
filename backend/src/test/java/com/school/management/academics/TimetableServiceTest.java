package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class TimetableServiceTest {
    @Test
    void overlappingTimeRangesConflictButAdjacentPeriodsDoNot() {
        assertTrue(TimetableService.overlaps(
            LocalTime.of(9, 30), LocalTime.of(10, 30),
            LocalTime.of(10, 0), LocalTime.of(11, 0)));
        assertFalse(TimetableService.overlaps(
            LocalTime.of(9, 0), LocalTime.of(10, 0),
            LocalTime.of(10, 0), LocalTime.of(11, 0)));
    }
}