package com.smartresolve.repository;

import com.smartresolve.entity.Ticket;
import com.smartresolve.enums.TicketPriority;
import com.smartresolve.enums.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TicketSpecification {

    private TicketSpecification() {
    }

    public static Specification<Ticket> employeeId(UUID employeeId) {
        return (root, query, cb) ->
                employeeId == null ? cb.conjunction()
                        : cb.equal(root.get("employee").get("id"), employeeId);
    }

    public static Specification<Ticket> assignedEngineerId(UUID engineerId) {
        return (root, query, cb) ->
                engineerId == null ? cb.conjunction()
                        : cb.equal(root.get("assignedEngineer").get("id"), engineerId);
    }

    public static Specification<Ticket> status(TicketStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> priority(TicketPriority priority) {
        return (root, query, cb) ->
                priority == null ? cb.conjunction()
                        : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
