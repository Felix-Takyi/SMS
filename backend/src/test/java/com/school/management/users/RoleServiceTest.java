package com.school.management.users;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceTest {
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PermissionRepository permissions = mock(PermissionRepository.class);
    private final RoleService roleService = new RoleService(roles, permissions);

    @Test
    void replacePermissionsRejectsUnknownPermissionCodes() {
        Role admin = mock(Role.class);
        Permission studentView = mock(Permission.class);
        when(studentView.getCode()).thenReturn("STUDENT_VIEW");

        when(roles.findByCode("ADMIN")).thenReturn(Optional.of(admin));
        when(permissions.findAllByCodeIn(Set.of("STUDENT_VIEW", "UNKNOWN_PERMISSION")))
            .thenReturn(List.of(studentView));

        ReplacePermissionsRequest request = new ReplacePermissionsRequest(Set.of("STUDENT_VIEW", "UNKNOWN_PERMISSION"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> roleService.replacePermissions("ADMIN", request));

        assertEquals("One or more permission codes are invalid.", exception.getMessage());
    }

    @Test
    void replacePermissionsAssignsExactPermissionSet() {
        Role admin = mock(Role.class);
        Permission studentView = mock(Permission.class);
        Permission userManage = mock(Permission.class);
        when(studentView.getCode()).thenReturn("STUDENT_VIEW");
        when(userManage.getCode()).thenReturn("USER_MANAGE");

        when(admin.getId()).thenReturn(UUID.randomUUID());
        when(admin.getCode()).thenReturn("ADMIN");
        when(admin.getName()).thenReturn("Administrator");
        when(admin.getDescription()).thenReturn("Admin role");
        when(admin.isSystemRole()).thenReturn(true);
        when(admin.getPermissions()).thenReturn(Set.of(studentView, userManage));

        when(roles.findByCode("ADMIN")).thenReturn(Optional.of(admin));
        when(permissions.findAllByCodeIn(Set.of("STUDENT_VIEW", "USER_MANAGE")))
            .thenReturn(List.of(studentView, userManage));

        RoleResponse response = roleService.replacePermissions("ADMIN",
            new ReplacePermissionsRequest(Set.of("STUDENT_VIEW", "USER_MANAGE")));

        assertEquals(List.of("STUDENT_VIEW", "USER_MANAGE"), response.permissions());
    }
}
