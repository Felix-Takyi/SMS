package com.school.management.security;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.school.management.users.AppUser;
import com.school.management.users.AppUserRepository;

class PasswordChangeRequiredFilterTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsStaticAppShellForTemporaryPasswordUser() throws Exception {
        AppUser user = temporaryUser();
        authenticate(user);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServletPath("/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] continued = {false};

        filter.doFilterInternal(request, response, (req, res) -> continued[0] = true);

        assertTrue(continued[0]);
        assertEquals(200, response.getStatus());
        verify(users, never()).findByUsernameIgnoreCase(user.getUsername());
    }

    @Test
    void blocksOtherApiRequestsUntilTemporaryPasswordChanges() throws Exception {
        AppUser user = temporaryUser();
        authenticate(user);
        when(users.findByUsernameIgnoreCase(user.getUsername())).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students");
        request.setServletPath("/api/v1/students");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] continued = {false};

        filter.doFilterInternal(request, response, (req, res) -> continued[0] = true);

        assertFalse(continued[0]);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
    }

    private AppUser temporaryUser() {
        AppUser user = new AppUser("newuser", "New User", null, "encoded-password");
        user.requirePasswordChange();
        return user;
    }

    private void authenticate(AppUser user) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
