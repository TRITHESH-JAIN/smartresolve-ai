package com.smartresolve.repository;

import com.smartresolve.entity.SupportTeamRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupportTeamRatingRepository
        extends JpaRepository<SupportTeamRating, UUID> {

    Optional<SupportTeamRating> findByTicket_Id(UUID ticketId);
}
