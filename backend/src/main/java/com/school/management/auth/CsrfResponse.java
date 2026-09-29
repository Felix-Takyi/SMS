package com.school.management.auth;

public record CsrfResponse(String headerName, String parameterName, String token) {
}
