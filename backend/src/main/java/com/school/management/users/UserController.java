package com.school.management.users;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;
    private final RoleService roleService;

    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public org.springframework.data.domain.Page<UserResponse> users(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return userService.listUsers(PageRequest.of(page, size, Sort.by("username").ascending()));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse replaceRoles(@PathVariable UUID id,
                                     @Valid @RequestBody ReplaceRolesRequest request) {
        return userService.replaceUserRoles(id, request);
    }

    @PutMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void resetPassword(@PathVariable UUID id,
                              @AuthenticationPrincipal com.school.management.users.AppUser actor,
                              @Valid @RequestBody ResetUserPasswordRequest request) {
        userService.resetPassword(id, actor.getId(), request.temporaryPassword());
    }

    @PostMapping("/users/{id}/activate")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse activate(@PathVariable UUID id) {
        return userService.setEnabled(id, true);
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse deactivate(@PathVariable UUID id) {
        return userService.setEnabled(id, false);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE') or hasAuthority('USER_MANAGE')")
    public java.util.List<RoleResponse> roles() {
        return roleService.listRoles();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public java.util.List<PermissionResponse> permissions() {
        return roleService.listPermissions();
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_ASSIGN')")
    public RoleResponse replacePermissions(@PathVariable String roleCode,
                                           @Valid @RequestBody ReplacePermissionsRequest request) {
        return roleService.replacePermissions(roleCode, request);
    }
}
