package com.school.management.users;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser implements UserDetails {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 120, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected AppUser() {
    }

    public AppUser(String username, String displayName, String email, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(permission -> (GrantedAuthority) permission)
            .distinct()
            .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void replaceRoles(Set<Role> assignedRoles) {
        roles.clear();
        roles.addAll(assignedRoles);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangeRequired = false;
        this.updatedAt = Instant.now();
    }

    public void requirePasswordChange() {
        this.passwordChangeRequired = true;
        this.updatedAt = Instant.now();
    }

    public void setTemporaryPasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangeRequired = true;
        this.updatedAt = Instant.now();
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void registerFailedLogin(Instant now, int maximumAttempts, Instant lockExpiry) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximumAttempts) {
            lockedUntil = lockExpiry;
            failedLoginAttempts = 0;
        }
        updatedAt = now;
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
