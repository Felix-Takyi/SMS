package com.school.management.users;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName, String email,
                           boolean enabled, List<String> roles) {
}
