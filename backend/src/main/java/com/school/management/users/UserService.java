package com.school.management.users;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.audit.LoginAttemptRecorder;

@Service
public class UserService {
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptRecorder loginAttemptRecorder;

    public UserService(AppUserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                       LoginAttemptRecorder loginAttemptRecorder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptRecorder = loginAttemptRecorder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return users.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        validatePassword(request.password());
        if (users.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new IllegalArgumentException("A user with that username already exists.");
        }
        Set<Role> assignedRoles = loadRoles(request.roleCodes());
        AppUser user = new AppUser(request.username().trim(), request.displayName().trim(),
            request.email() == null ? null : request.email().trim(),
            passwordEncoder.encode(request.password()));
        user.requirePasswordChange();
        user.replaceRoles(assignedRoles);
        try {
            return toUserResponse(users.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("A user with that username or email already exists.");
        }
    }

    @Transactional
    public UserResponse replaceUserRoles(UUID userId, ReplaceRolesRequest request) {
        AppUser user = users.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        user.replaceRoles(loadRoles(request.roleCodes()));
        return toUserResponse(users.save(user));
    }

    @Transactional
    public UserResponse setEnabled(UUID userId, boolean enabled) {
        AppUser user = users.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        user.setEnabled(enabled);
        return toUserResponse(users.save(user));
    }

    @Transactional
    public void resetPassword(UUID userId, UUID actorId, String temporaryPassword) {
        validatePassword(temporaryPassword);
        if (userId.equals(actorId)) {
            throw new IllegalArgumentException("Use your own account settings to change your password.");
        }
        AppUser target = users.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        AppUser actor = users.findById(actorId)
            .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        loginAttemptRecorder.recordPasswordReset(actor, target, passwordEncoder.encode(temporaryPassword));
    }

    private Set<Role> loadRoles(Set<String> codes) {
        List<Role> found = roles.findAllByCodeIn(codes);
        if (found.size() != codes.size()) {
            throw new IllegalArgumentException("One or more role codes are invalid.");
        }
        return Set.copyOf(found);
    }

    private void validatePassword(String password) {
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < 8 || bytes > 72) {
            throw new IllegalArgumentException("Password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
            user.isEnabled(), user.isPasswordChangeRequired(),
            user.getRoles().stream().map(Role::getCode).sorted(Comparator.naturalOrder()).toList());
    }
}
