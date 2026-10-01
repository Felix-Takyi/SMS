package com.school.management.users;

import java.util.List;
import java.util.UUID;

public record RoleResponse(UUID id, String code, String name, String description,
                           boolean systemRole, List<String> permissions) {
}
