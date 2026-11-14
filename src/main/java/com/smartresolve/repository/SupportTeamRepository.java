package com.smartresolve.repository;


import com.smartresolve.entity.SupportTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupportTeamRepository
        extends JpaRepository<SupportTeam, UUID> {

    Optional<SupportTeam> findByTeamName(String teamName);

    boolean existsByTeamName(String teamName);
}