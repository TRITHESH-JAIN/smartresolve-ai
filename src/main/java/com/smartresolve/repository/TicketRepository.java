package com.smartresolve.repository;


import com.smartresolve.entity.Ticket;
import com.smartresolve.enums.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository
        extends JpaRepository<Ticket, UUID>,
                JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByEmployee_IdOrderByCreatedAtDesc(UUID employeeId);

    List<Ticket> findByAssignedEngineer_IdOrderByCreatedAtDesc(
            UUID engineerId
    );

    List<Ticket> findByTeam_IdOrderByCreatedAtDesc(UUID teamId);

    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    Optional<Ticket> findByIdAndEmployee_Id(
            UUID ticketId,
            UUID employeeId
    );

    long countByStatus(TicketStatus status);

    long countByEmployee_Id(UUID employeeId);

    long countByAssignedEngineer_Id(UUID engineerId);

    long countByPriority(com.smartresolve.enums.TicketPriority priority);

    long countByStatusIn(
            Collection<com.smartresolve.enums.TicketStatus> statuses
    );

    long countByEmployee_IdAndStatus(
            UUID employeeId,
            com.smartresolve.enums.TicketStatus status
    );

    long countByAssignedEngineer_IdAndStatus(
            UUID engineerId,
            com.smartresolve.enums.TicketStatus status
    );

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.status IN :statuses
              AND LOWER(CONCAT(
                  COALESCE(t.title, ''), ' ',
                  COALESCE(t.description, ''), ' ',
                  COALESCE(t.resolutionNotes, '')
              )) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY t.resolvedAt DESC
            """)
    List<Ticket> searchResolvedTickets(
            @Param("keyword") String keyword,
            @Param("statuses") Collection<TicketStatus> statuses,
            Pageable pageable
    );
}