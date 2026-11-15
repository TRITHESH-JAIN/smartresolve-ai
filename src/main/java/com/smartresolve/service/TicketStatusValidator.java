package com.smartresolve.service;

import com.smartresolve.enums.TicketStatus;
import org.springframework.stereotype.Component;

@Component
public class TicketStatusValidator {

    public void validate(TicketStatus current, TicketStatus next) {
        // All values declared by TicketStatus are accepted by the status API.
    }
}
