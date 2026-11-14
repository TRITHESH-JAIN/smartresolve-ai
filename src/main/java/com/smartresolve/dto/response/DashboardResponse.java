package com.smartresolve.dto.response;

import java.util.Map;

public record DashboardResponse(
        long totalTickets,
        long newTickets,
        long openTickets,
        long assignedTickets,
        long inProgressTickets,
        long onHoldTickets,
        long resolvedTickets,
        long closedTickets,
        long criticalTickets,
        Map<String, Long> ticketsByPriority,
        Map<String, Long> ticketsByStatus
) {
}
