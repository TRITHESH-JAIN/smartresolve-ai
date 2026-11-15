package com.smartresolve.service.ticket;

import com.smartresolve.dto.request.*;
import com.smartresolve.dto.response.*;
import com.smartresolve.entity.*;
import com.smartresolve.enums.*;
import com.smartresolve.repository.*;
import com.smartresolve.service.TicketStatusValidator;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SupportTeamRepository teamRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;
    private final TicketStatusValidator statusValidator;
    private final SupportTeamRatingRepository supportTeamRatingRepository;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketCategoryRepository categoryRepository,
            SupportTeamRepository teamRepository,
            TicketCommentRepository commentRepository,
            TicketHistoryRepository historyRepository,
            TicketStatusValidator statusValidator,
            SupportTeamRatingRepository supportTeamRatingRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.teamRepository = teamRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.statusValidator = statusValidator;
        this.supportTeamRatingRepository = supportTeamRatingRepository;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, String email) {
        User employee = getUser(email);
        TicketCategory category = null;
        SupportTeam team = null;

        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            if (!category.isActive()) {
                throw new IllegalArgumentException("Category is inactive");
            }
            team = category.getSupportTeam();
        }

        Ticket ticket = Ticket.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .category(category)
                .team(team)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.NEW)
                .employee(employee)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        saveHistory(savedTicket, "TICKET_CREATED", null, null, "NEW",
                employee, "Ticket submitted by employee");
        return mapTicket(savedTicket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(
            String email,
            TicketStatus status,
            TicketPriority priority,
            UUID categoryId,
            Pageable pageable
    ) {
        User currentUser = getUser(email);
        Specification<Ticket> specification = (root, query, cb) -> cb.conjunction();

        if (currentUser.getRole() == Role.EMPLOYEE) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("employee").get("id"), currentUser.getId()));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("priority"), priority));
        }
        if (categoryId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        return ticketRepository.findAll(specification, pageable).map(this::mapTicket);
    }

    @Transactional(readOnly = true)
    public TicketPageResponse getTicketsPaginated(
            String email,
            int page,
            int size,
            TicketStatus status,
            TicketPriority priority,
            String keyword,
            UUID categoryId
    ) {
        User user = getUser(email);

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid page or size. Size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<Ticket> specification =
                Specification.where(TicketSpecification.status(status))
                        .and(TicketSpecification.priority(priority))
                        .and(TicketSpecification.search(keyword))
                        .and((root, query, cb) -> categoryId == null
                                ? cb.conjunction()
                                : cb.equal(root.get("category").get("id"), categoryId));

        if (user.getRole() == Role.EMPLOYEE) {
            specification = specification.and(
                    TicketSpecification.employeeId(user.getId()));
        } else if (user.getRole() == Role.SUPPORT_ENGINEER) {
            specification = specification.and(
                    TicketSpecification.assignedEngineerId(user.getId()));
        }

        Page<Ticket> ticketPage = ticketRepository.findAll(specification, pageable);

        return new TicketPageResponse(
                ticketPage.getContent().stream()
                        .map(this::mapTicket)
                        .toList(),
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isFirst(),
                ticketPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId, String email) {
        User currentUser = getUser(email);
        Ticket ticket = getTicketEntity(ticketId);
        ensureCanView(ticket, currentUser);
        return mapTicket(ticket);
    }

    @Transactional
    public TicketResponse updateTicket(
            UUID ticketId,
            UpdateTicketRequest request,
            String email
    ) {
        User currentUser = getUser(email);
        Ticket ticket = getTicketEntity(ticketId);

        if (currentUser.getRole() == Role.EMPLOYEE) {
            ensureEmployeeOwnsTicket(ticket, currentUser);
            if (ticket.getStatus() != TicketStatus.NEW
                    && ticket.getStatus() != TicketStatus.REOPENED) {
                throw new AccessDeniedException(
                        "You cannot edit a ticket after processing has started");
            }
        } else {
            ensureSupportOrAdmin(currentUser);
        }

        String oldTitle = ticket.getTitle();
        String oldDescription = ticket.getDescription();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());

        if (request.priority() != null) {
            ensureSupportOrAdmin(currentUser);
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(request.priority());
            saveHistory(ticket, "TICKET_UPDATED", "priority",
                    String.valueOf(oldPriority), String.valueOf(request.priority()),
                    currentUser, "Priority updated");
        }

        if (request.categoryId() != null) {
            TicketCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            if (!category.isActive()) {
                throw new IllegalArgumentException("Category is inactive");
            }
            ticket.setCategory(category);
            ticket.setTeam(category.getSupportTeam());
        }

        saveHistory(ticket, "TICKET_UPDATED", "title", oldTitle, ticket.getTitle(),
                currentUser, "Ticket title updated");
        saveHistory(ticket, "TICKET_UPDATED", "description", oldDescription,
                ticket.getDescription(), currentUser, "Ticket description updated");
        return mapTicket(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(
            UUID ticketId,
            UpdateTicketStatusRequest request,
            String email
    ) {
        User currentUser = getUser(email);
        ensureSupportOrAdmin(currentUser);
        Ticket ticket = getTicketEntity(ticketId);
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.status();
        statusValidator.validate(oldStatus, newStatus);
        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.REOPENED) {
            ticket.setResolvedAt(null);
            ticket.setClosedAt(null);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        saveHistory(savedTicket, "STATUS_CHANGED", "status",
                oldStatus.name(), newStatus.name(), currentUser, request.remarks());
        return mapTicket(savedTicket);
    }

    @Transactional
    public TicketResponse rateSupportTeam(
            UUID ticketId,
            RateSupportTeamRequest request,
            String email
    ) {
        User employee = getUser(email);
        Ticket ticket = getTicketEntity(ticketId);

        if (!ticket.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException(
                    "Only the ticket employee can rate the support team");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED
                && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A support team can be rated only after the ticket is resolved or closed"
            );
        }
        if (ticket.getTeam() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This ticket has no assigned support team to rate"
            );
        }

        SupportTeamRating rating = supportTeamRatingRepository
                .findByTicket_Id(ticketId)
                .orElseGet(() -> SupportTeamRating.builder()
                        .ticket(ticket)
                        .supportTeam(ticket.getTeam())
                        .employee(employee)
                        .build());
        rating.setRating(request.rating());
        rating.setFeedback(request.feedback() == null
                ? null : request.feedback().trim());
        supportTeamRatingRepository.save(rating);
        ticket.setSupportTeamRating(rating);
        return mapTicket(ticket);
    }

    @Transactional
    public TicketResponse assignTicket(
            UUID ticketId,
            AssignTicketRequest request,
            String email
    ) {
        User currentUser = getUser(email);
        ensureAdmin(currentUser);
        Ticket ticket = getTicketEntity(ticketId);
        User engineer = userRepository.findById(request.assignedEngineerId())
                .orElseThrow(() -> new IllegalArgumentException("Engineer not found"));

        if (engineer.getRole() != Role.SUPPORT_ENGINEER || !engineer.isActive()) {
            throw new IllegalArgumentException(
                    "Selected user is not an active support engineer");
        }

        UUID oldEngineerId = ticket.getAssignedEngineer() == null
                ? null : ticket.getAssignedEngineer().getId();
        SupportTeam team = ticket.getTeam();

        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found"));
            if (!team.isActive()) {
                throw new IllegalArgumentException("Team is inactive");
            }
        }

        if (team != null && engineer.getTeam() != null
                && !team.getId().equals(engineer.getTeam().getId())) {
            throw new IllegalArgumentException(
                    "Engineer does not belong to the selected team");
        }

        ticket.setAssignedEngineer(engineer);
        ticket.setTeam(team);
        if (ticket.getStatus() == TicketStatus.NEW
                || ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        saveHistory(savedTicket, "TICKET_ASSIGNED", "assignedEngineer",
                oldEngineerId == null ? null : oldEngineerId.toString(),
                engineer.getId().toString(), currentUser, request.remarks());
        return mapTicket(savedTicket);
    }

    @Transactional
    public CommentResponse addComment(
            UUID ticketId,
            AddCommentRequest request,
            String email
    ) {
        User currentUser = getUser(email);
        Ticket ticket = getTicketEntity(ticketId);
        ensureCanView(ticket, currentUser);
        if (request.internalNote()) {
            ensureSupportOrAdmin(currentUser);
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .user(currentUser)
                .message(request.message().trim())
                .internalNote(request.internalNote())
                .build();
        return mapComment(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID ticketId, String email) {
        User currentUser = getUser(email);
        Ticket ticket = getTicketEntity(ticketId);
        ensureCanView(ticket, currentUser);

        List<TicketComment> comments = currentUser.getRole() == Role.EMPLOYEE
                ? commentRepository.findByTicket_IdAndInternalNoteFalseOrderByCreatedAtAsc(ticketId)
                : commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId);
        return comments.stream().map(this::mapComment).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getHistory(UUID ticketId, String email) {
        User currentUser = getUser(email);
        ensureSupportOrAdmin(currentUser);
        getTicketEntity(ticketId);
        return historyRepository.findByTicket_IdOrderByCreatedAtDesc(ticketId)
                .stream().map(this::mapHistory).toList();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Ticket getTicketEntity(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    private void ensureCanView(Ticket ticket, User user) {
        if (user.getRole() == Role.EMPLOYEE) {
            ensureEmployeeOwnsTicket(ticket, user);
        } else {
            ensureSupportOrAdmin(user);
        }
    }

    private void ensureEmployeeOwnsTicket(Ticket ticket, User user) {
        if (!ticket.getEmployee().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to access this ticket");
        }
    }

    private void ensureSupportOrAdmin(User user) {
        if (user.getRole() != Role.SUPPORT_ENGINEER
                && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action");
        }
    }

    private void ensureAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only administrators can assign tickets");
        }
    }

    private void saveHistory(
            Ticket ticket,
            String action,
            String fieldName,
            String oldValue,
            String newValue,
            User performedBy,
            String remarks
    ) {
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .remarks(remarks)
                .build();
        historyRepository.save(history);
    }

    private TicketResponse mapTicket(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory() == null ? null : ticket.getCategory().getCategoryName(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getEmployee().getId(),
                ticket.getEmployee().getFullName(),
                ticket.getAssignedEngineer() == null
                        ? null : ticket.getAssignedEngineer().getId(),
                ticket.getAssignedEngineer() == null
                        ? null : ticket.getAssignedEngineer().getFullName(),
                ticket.getTeam() == null ? null : ticket.getTeam().getId(),
                ticket.getTeam() == null ? null : ticket.getTeam().getTeamName(),
                ticket.getResolutionNotes(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getSupportTeamRating() == null
                        ? null : ticket.getSupportTeamRating().getRating(),
                ticket.getSupportTeamRating() == null
                        ? null : ticket.getSupportTeamRating().getFeedback()
        );
    }

    private CommentResponse mapComment(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getUser().getId(),
                comment.getUser().getFullName(),
                comment.getMessage(),
                comment.isInternalNote(),
                comment.getCreatedAt()
        );
    }

    private TicketHistoryResponse mapHistory(TicketHistory history) {
        return new TicketHistoryResponse(
                history.getId(),
                history.getTicket().getId(),
                history.getAction(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getPerformedBy().getId(),
                history.getPerformedBy().getFullName(),
                history.getRemarks(),
                history.getCreatedAt()
        );
    }
}
