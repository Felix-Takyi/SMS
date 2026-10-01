package com.school.management.users;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String description) {
    static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription());
    }
}
