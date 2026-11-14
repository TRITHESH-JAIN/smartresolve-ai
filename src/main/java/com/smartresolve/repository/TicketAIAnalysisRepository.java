package com.smartresolve.repository;

import com.smartresolve.entity.TicketAIAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketAIAnalysisRepository
        extends JpaRepository<TicketAIAnalysis, UUID> {

    List<TicketAIAnalysis> findByTicket_IdOrderByCreatedAtDesc(
            UUID ticketId
    );

    Optional<TicketAIAnalysis> findFirstByTicket_IdOrderByCreatedAtDesc(
            UUID ticketId
    );
}