package com.school.management.users;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ReplacePermissionsRequest(@NotNull Set<String> permissionCodes) {
}
