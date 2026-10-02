package com.school.management.academics;

import java.util.UUID;

import com.school.management.users.AppUser;

public record TeacherOptionResponse(UUID id, String displayName, String username) {
    public static TeacherOptionResponse from(AppUser user) {
        return new TeacherOptionResponse(user.getId(), user.getDisplayName(), user.getUsername());
    }
}