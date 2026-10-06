package com.school.management.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
    @NotBlank @Size(min = 8, max = 72) String temporaryPassword
) {
}