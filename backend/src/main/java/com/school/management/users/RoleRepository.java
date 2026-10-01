package com.school.management.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByCode(String code);

    List<Role> findAllByCodeIn(Iterable<String> codes);

    List<Role> findAllByOrderByNameAsc();
}
