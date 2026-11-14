package com.smartresolve.service;

import com.smartresolve.enums.TicketStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TicketStatusValidator {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS =
            new EnumMap<>(TicketStatus.class);

    static {
        TRANSITIONS.put(TicketStatus.NEW,
                EnumSet.of(TicketStatus.OPEN, TicketStatus.ASSIGNED));
        TRANSITIONS.put(TicketStatus.AI_ANALYSIS,
                EnumSet.of(TicketStatus.OPEN));
        TRANSITIONS.put(TicketStatus.OPEN,
                EnumSet.of(TicketStatus.ASSIGNED,
                        TicketStatus.IN_PROGRESS,
                        TicketStatus.ON_HOLD));
        TRANSITIONS.put(TicketStatus.ASSIGNED,
                EnumSet.of(TicketStatus.IN_PROGRESS,
                        TicketStatus.ON_HOLD));
        TRANSITIONS.put(TicketStatus.IN_PROGRESS,
                EnumSet.of(TicketStatus.ON_HOLD,
                        TicketStatus.RESOLVED));
        TRANSITIONS.put(TicketStatus.ON_HOLD,
                EnumSet.of(TicketStatus.IN_PROGRESS,
                        TicketStatus.RESOLVED));
        TRANSITIONS.put(TicketStatus.RESOLVED,
                EnumSet.of(TicketStatus.CLOSED,
                        TicketStatus.REOPENED));
        TRANSITIONS.put(TicketStatus.CLOSED,
                EnumSet.of(TicketStatus.REOPENED));
        TRANSITIONS.put(TicketStatus.REOPENED,
                EnumSet.of(TicketStatus.ASSIGNED,
                        TicketStatus.IN_PROGRESS,
                        TicketStatus.ON_HOLD));
    }

    public void validate(TicketStatus current, TicketStatus next) {
        if (current == next) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket is already in this status");
        }

        Set<TicketStatus> allowed = TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition: " + current + " -> " + next);
        }
    }
}
