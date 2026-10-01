package com.school.management.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;

    public RoleService(RoleRepository roles, PermissionRepository permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roles.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissions.findAllByOrderByCodeAsc().stream().map(PermissionResponse::from).toList();
    }

    @Transactional
    public RoleResponse replacePermissions(String roleCode, ReplacePermissionsRequest request) {
        Role role = roles.findByCode(roleCode)
            .orElseThrow(() -> new IllegalArgumentException("Role was not found."));
        Set<String> codes = request.permissionCodes();
        List<Permission> grants = permissions.findAllByCodeIn(codes);
        if (grants.size() != codes.size()) {
            throw new IllegalArgumentException("One or more permission codes are invalid.");
        }
        role.replacePermissions(Set.copyOf(grants));
        return toResponse(role);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(),
            role.isSystemRole(), role.getPermissions().stream().map(Permission::getCode)
                .sorted(Comparator.naturalOrder()).toList());
    }
}
