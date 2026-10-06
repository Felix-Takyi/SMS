package com.school.management.users;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService = new UserService(users, roles, passwordEncoder);

    @Test
    void createUserRejectsPasswordBelowRequiredLength() {
        CreateUserRequest request = new CreateUserRequest(
            "alice",
            "Alice Example",
            "alice@example.com",
            "short",
            Set.of("SUPER_ADMIN")
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.createUser(request));

        assertTrue(exception.getMessage().contains("at least 8"));
    }

    @Test
    void createUserEncodesPasswordAndAssignsRoles() {
        Role superAdmin = mock(Role.class);
        when(superAdmin.getCode()).thenReturn("SUPER_ADMIN");
        when(roles.findAllByCodeIn(Set.of("SUPER_ADMIN"))).thenReturn(List.of(superAdmin));
        when(users.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserRequest request = new CreateUserRequest(
            "alice",
            "Alice Example",
            "alice@example.com",
            "Passw0rd",
            Set.of("SUPER_ADMIN")
        );

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(userCaptor.capture());

        AppUser savedUser = userCaptor.getValue();
        assertEquals("alice", response.username());
        assertEquals("Alice Example", response.displayName());
        assertEquals(List.of("SUPER_ADMIN"), response.roles());
        assertTrue(passwordEncoder.matches("Passw0rd", savedUser.getPassword()));
        assertEquals(Set.of("SUPER_ADMIN"), savedUser.getRoles().stream().map(Role::getCode).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void setEnabledUpdatesUserStatus() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser("bob", "Bob Example", "bob@example.com", passwordEncoder.encode("StrongPassword!123"));
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.setEnabled(userId, false);

        assertEquals(false, response.enabled());
        assertEquals(false, user.isEnabled());
    }
}
