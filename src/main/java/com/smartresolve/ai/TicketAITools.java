package com.smartresolve.ai;

import com.smartresolve.entity.SupportTeam;
import com.smartresolve.entity.Ticket;
import com.smartresolve.entity.TicketCategory;
import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import com.smartresolve.enums.TicketStatus;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.TicketCategoryRepository;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class TicketAITools {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SupportTeamRepository teamRepository;

    public TicketAITools(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketCategoryRepository categoryRepository,
            SupportTeamRepository teamRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.teamRepository = teamRepository;
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new IllegalStateException("Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"));
    }

    @Tool(
            name = "search_similar_resolved_tickets",
            description = """
                    Search previously resolved or closed support tickets using
                    a keyword describing the technical problem. Return only
                    relevant, non-sensitive issue summaries and resolution notes.
                    """
    )
    @Transactional(readOnly = true)
    public String searchSimilarResolvedTickets(
            @ToolParam(description =
                    "Short technical keyword or phrase describing the issue")
            String keyword
    ) {
        getCurrentUser();

        if (keyword == null || keyword.isBlank()) {
            return "Please provide a keyword describing the issue.";
        }
        String cleanedKeyword = keyword.trim();
        if (cleanedKeyword.length() > 100) {
            return "Search keyword must not exceed 100 characters.";
        }

        List<Ticket> tickets = ticketRepository.searchResolvedTickets(
                cleanedKeyword,
                List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED),
                PageRequest.of(0, 5)
        );
        if (tickets.isEmpty()) {
            return "No matching resolved tickets were found.";
        }

        StringBuilder result = new StringBuilder();
        for (Ticket ticket : tickets) {
            result.append("\nIssue: ").append(ticket.getTitle())
                    .append("\nCategory: ")
                    .append(ticket.getCategory() == null
                            ? "Not specified"
                            : ticket.getCategory().getCategoryName())
                    .append("\nPriority: ").append(ticket.getPriority())
                    .append("\nResolution: ")
                    .append(ticket.getResolutionNotes() == null
                            ? "No resolution notes available"
                            : ticket.getResolutionNotes())
                    .append("\n---\n");
        }
        return result.toString();
    }

    @Tool(
            name = "find_support_team",
            description = """
                    Find the active support team responsible for a given ticket
                    category. Use this when an employee asks which team should
                    handle an issue.
                    """
    )
    @Transactional(readOnly = true)
    public String findSupportTeam(
            @ToolParam(description =
                    "Ticket category name, for example Network or Access")
            String categoryName
    ) {
        getCurrentUser();

        if (categoryName == null || categoryName.isBlank()) {
            return "Please provide a ticket category.";
        }

        return categoryRepository.findByActiveTrueOrderByCategoryNameAsc()
                .stream()
                .filter(category -> category.getCategoryName()
                        .equalsIgnoreCase(categoryName.trim()))
                .filter(category -> category.getSupportTeam() != null)
                .filter(category -> category.getSupportTeam().isActive())
                .findFirst()
                .map(category -> {
                    SupportTeam team = category.getSupportTeam();
                    return "Category: " + category.getCategoryName()
                            + "\nResponsible team: " + team.getTeamName()
                            + "\nTeam description: "
                            + (team.getDescription() == null
                            ? "Not available" : team.getDescription());
                })
                .orElse("No active support team was found for this category.");
    }

    @Tool(
            name = "get_ticket_status",
            description = """
                    Retrieve the current status and basic details of a ticket.
                    Employees can access only their own tickets. Support
                    engineers can access tickets assigned to them.
                    Administrators can access any ticket.
                    """
    )
    @Transactional(readOnly = true)
    public String getTicketStatus(
            @ToolParam(description = "UUID of the support ticket")
            String ticketId
    ) {
        User currentUser = getCurrentUser();
        UUID id;
        try {
            id = UUID.fromString(ticketId);
        } catch (Exception exception) {
            return "Invalid ticket ID.";
        }

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return "Ticket not found.";
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (ticket.getEmployee() == null
                    || !ticket.getEmployee().getId().equals(currentUser.getId())) {
                return "You are not authorized to access this ticket.";
            }
        } else if (currentUser.getRole() == Role.SUPPORT_ENGINEER) {
            if (ticket.getAssignedEngineer() == null
                    || !ticket.getAssignedEngineer().getId()
                    .equals(currentUser.getId())) {
                return "You are not authorized to access this ticket.";
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            return "You are not authorized to access this ticket.";
        }

        return "Ticket ID: " + ticket.getId()
                + "\nTitle: " + ticket.getTitle()
                + "\nStatus: " + ticket.getStatus()
                + "\nPriority: " + ticket.getPriority()
                + "\nCategory: "
                + (ticket.getCategory() == null
                ? "Not specified"
                : ticket.getCategory().getCategoryName())
                + "\nResolution: "
                + (ticket.getResolutionNotes() == null
                ? "Not resolved yet"
                : ticket.getResolutionNotes());
    }
}
