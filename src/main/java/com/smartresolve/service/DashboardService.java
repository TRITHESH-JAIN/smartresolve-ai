package com.smartresolve.service;

import com.smartresolve.dto.response.DashboardResponse;
import com.smartresolve.entity.Ticket;
import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import com.smartresolve.enums.TicketPriority;
import com.smartresolve.enums.TicketStatus;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public DashboardService(
            TicketRepository ticketRepository,
            UserRepository userRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        User user = getCurrentUser();
        List<Ticket> tickets;

        if (user.getRole() == Role.ADMIN) {
            tickets = ticketRepository.findAll();
        } else if (user.getRole() == Role.SUPPORT_ENGINEER) {
            tickets = ticketRepository
                    .findByAssignedEngineer_IdOrderByCreatedAtDesc(user.getId());
        } else {
            tickets = ticketRepository
                    .findByEmployee_IdOrderByCreatedAtDesc(user.getId());
        }

        Map<String, Long> statusCounts = new HashMap<>();
        Map<String, Long> priorityCounts = new HashMap<>();

        for (TicketStatus status : TicketStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }
        for (TicketPriority priority : TicketPriority.values()) {
            priorityCounts.put(priority.name(), 0L);
        }

        for (Ticket ticket : tickets) {
            if (ticket.getStatus() != null) {
                statusCounts.merge(ticket.getStatus().name(), 1L, Long::sum);
            }
            if (ticket.getPriority() != null) {
                priorityCounts.merge(ticket.getPriority().name(), 1L, Long::sum);
            }
        }

        return new DashboardResponse(
                tickets.size(),
                count(statusCounts, TicketStatus.NEW),
                count(statusCounts, TicketStatus.OPEN),
                count(statusCounts, TicketStatus.ASSIGNED),
                count(statusCounts, TicketStatus.IN_PROGRESS),
                count(statusCounts, TicketStatus.ON_HOLD),
                count(statusCounts, TicketStatus.RESOLVED),
                count(statusCounts, TicketStatus.CLOSED),
                priorityCounts.getOrDefault(
                        TicketPriority.CRITICAL.name(), 0L),
                priorityCounts,
                statusCounts
        );
    }

    private long count(Map<String, Long> counts, TicketStatus status) {
        return counts.getOrDefault(status.name(), 0L);
    }
}
