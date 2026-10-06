package com.school.management.auth;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
    UUID id,
    String username,
    String displayName,
    List<String> roles,
    List<String> permissions,
    boolean passwordChangeRequired
) {
}
