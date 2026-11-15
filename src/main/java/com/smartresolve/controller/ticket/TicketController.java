package com.smartresolve.controller.ticket;

import com.smartresolve.dto.request.*;
import com.smartresolve.dto.response.*;
import com.smartresolve.enums.*;
import com.smartresolve.service.ticket.TicketService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ticketService.createTicket(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be >= 0 and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ticketService.getTickets(
                userDetails.getUsername(), status, priority, categoryId, pageable));
    }

    @GetMapping("/search")
    public TicketPageResponse searchTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            Authentication authentication
    ) {
        return ticketService.getTicketsPaginated(
                authentication.getName(),
                page,
                size,
                status,
                priority,
                keyword,
                categoryId
        );
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.getTicket(
                ticketId, userDetails.getUsername()));
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.updateTicket(
                ticketId, request, userDetails.getUsername()));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.updateStatus(
                ticketId, request, userDetails.getUsername()));
    }

    @PostMapping("/{ticketId}/rating")
    public ResponseEntity<TicketResponse> rateSupportTeam(
            @PathVariable UUID ticketId,
            @Valid @RequestBody RateSupportTeamRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.rateSupportTeam(
                ticketId, request, userDetails.getUsername()));
    }

    @PostMapping("/{ticketId}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.assignTicket(
                ticketId, request, userDetails.getUsername()));
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ticketService.addComment(
                        ticketId, request, userDetails.getUsername()));
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.getComments(
                ticketId, userDetails.getUsername()));
    }

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<List<TicketHistoryResponse>> getHistory(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ticketService.getHistory(
                ticketId, userDetails.getUsername()));
    }
}
