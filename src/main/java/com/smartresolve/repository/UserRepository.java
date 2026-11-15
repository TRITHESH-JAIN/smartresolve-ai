package com.smartresolve.repository;

import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "team")
    List<User> findByRoleAndActiveTrueOrderByFullNameAsc(Role role);

    @EntityGraph(attributePaths = "team")
    List<User> findByRoleAndActiveTrueAndTeam_IdOrderByFullNameAsc(
            Role role,
            UUID teamId
    );
}