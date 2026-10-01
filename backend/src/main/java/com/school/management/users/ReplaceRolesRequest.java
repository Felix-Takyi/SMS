package com.school.management.users;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record ReplaceRolesRequest(@NotEmpty Set<@NotBlank String> roleCodes) {
}
