package com.school.management.users;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where lower(user.username) = lower(:username)")
    Optional<AppUser> findLockedByUsername(@Param("username") String username);

    boolean existsByUsernameIgnoreCase(String username);
}
