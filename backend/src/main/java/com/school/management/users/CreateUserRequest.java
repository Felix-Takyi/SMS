package com.school.management.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
    @NotBlank @Size(max = 120) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
    @NotBlank @Size(max = 180) String displayName,
    @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 72) String password,
    @NotEmpty Set<@NotBlank String> roleCodes
) {
}
