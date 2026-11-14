package com.smartresolve.service;

import com.smartresolve.dto.ai.AIAnalysisResult;
import com.smartresolve.dto.response.AIAnalysisResponse;
import com.smartresolve.entity.SupportTeam;
import com.smartresolve.entity.Ticket;
import com.smartresolve.entity.TicketAIAnalysis;
import com.smartresolve.entity.TicketCategory;
import com.smartresolve.entity.User;
import com.smartresolve.enums.AIRecommendationStatus;
import com.smartresolve.enums.Role;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.TicketAIAnalysisRepository;
import com.smartresolve.repository.TicketCategoryRepository;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIAnalysisService {

    private final ChatClient chatClient;
    private final TicketRepository ticketRepository;
    private final TicketAIAnalysisRepository analysisRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SupportTeamRepository teamRepository;
    private final UserRepository userRepository;

    public AIAnalysisService(
            ChatClient.Builder chatClientBuilder,
            TicketRepository ticketRepository,
            TicketAIAnalysisRepository analysisRepository,
            TicketCategoryRepository categoryRepository,
            SupportTeamRepository teamRepository,
            UserRepository userRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.ticketRepository = ticketRepository;
        this.analysisRepository = analysisRepository;
        this.categoryRepository = categoryRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private Ticket getTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private void checkAccess(Ticket ticket, User user) {
        if (user.getRole() == Role.EMPLOYEE
                && !ticket.getEmployee().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot access this ticket");
        }
    }

    @Transactional
    public AIAnalysisResponse analyzeTicket(UUID ticketId, String email) {
        User user = getCurrentUser(email);
        Ticket ticket = getTicket(ticketId);
        checkAccess(ticket, user);
        log.info("Starting AI analysis for ticket {}", ticketId);

        List<TicketCategory> categories =
                categoryRepository.findByActiveTrueOrderByCategoryNameAsc();
        List<SupportTeam> teams = teamRepository.findAll()
                .stream()
                .filter(SupportTeam::isActive)
                .toList();

        String categoryOptions = categories.stream()
                .map(TicketCategory::getCategoryName)
                .collect(Collectors.joining(", "));
        String teamOptions = teams.stream()
                .map(SupportTeam::getTeamName)
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                You are SmartResolve AI, an IT support ticket analysis assistant.

                Analyze the employee's IT support ticket and return a structured
                recommendation.

                Rules:
                1. Do not invent technical facts.
                2. Use only the provided category names where possible.
                3. Use only the provided support team names where possible.
                4. Priority must be one of LOW, MEDIUM, HIGH, CRITICAL.
                5. CRITICAL means a major business outage, serious security
                   incident, or widespread service disruption.
                6. HIGH means an important user or business function is blocked.
                7. MEDIUM means a normal issue affecting limited functionality.
                8. LOW means a minor issue or general request.
                9. Troubleshooting steps must be safe and practical.
                10. Never ask the user to share passwords, OTPs, or secrets.
                11. Do not claim that you have fixed the issue.
                12. If information is insufficient, mention what needs
                    clarification in the summary or suggested response.
                13. Return troubleshooting steps as a list.

                Available categories: %s

                Available support teams: %s
                """.formatted(categoryOptions, teamOptions);

        String userPrompt = """
                Analyze this IT support ticket.

                Ticket title:
                %s

                Ticket description:
                %s

                Return:
                - A concise summary
                - Suggested category
                - Suggested priority
                - Suggested support team
                - Troubleshooting steps
                - Suggested response to the employee
                """.formatted(ticket.getTitle(), ticket.getDescription());

        AIAnalysisResult result;
        try {
            result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(AIAnalysisResult.class);
        } catch (Exception exception) {
            log.error("AI analysis failed for ticket {}", ticketId, exception);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI analysis is currently unavailable. Please try again later."
            );
        }

        if (result == null || result.summary() == null
                || result.suggestedPriority() == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI returned an incomplete analysis. Please retry."
            );
        }

        String troubleshootingSteps = result.troubleshootingSteps() == null
                ? ""
                : String.join(
                        System.lineSeparator(),
                        result.troubleshootingSteps());

        TicketAIAnalysis analysis = TicketAIAnalysis.builder()
                .ticket(ticket)
                .summary(result.summary())
                .suggestedCategory(result.suggestedCategory())
                .suggestedPriority(result.suggestedPriority())
                .suggestedTeam(result.suggestedTeam())
                .troubleshootingSteps(troubleshootingSteps)
                .suggestedResponse(result.suggestedResponse())
                .recommendationStatus(AIRecommendationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        TicketAIAnalysis saved = analysisRepository.save(analysis);
        log.info("AI analysis completed for ticket {}", ticketId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AIAnalysisResponse getLatestAnalysis(UUID ticketId, String email) {
        User user = getCurrentUser(email);
        Ticket ticket = getTicket(ticketId);
        checkAccess(ticket, user);

        TicketAIAnalysis analysis = analysisRepository
                .findFirstByTicket_IdOrderByCreatedAtDesc(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI analysis not found for this ticket"));
        return toResponse(analysis);
    }

    private AIAnalysisResponse toResponse(TicketAIAnalysis analysis) {
        return new AIAnalysisResponse(
                analysis.getId(),
                analysis.getTicket().getId(),
                analysis.getSummary(),
                analysis.getSuggestedCategory(),
                analysis.getSuggestedPriority(),
                analysis.getSuggestedTeam(),
                analysis.getTroubleshootingSteps(),
                analysis.getSuggestedResponse(),
                analysis.getRecommendationStatus(),
                analysis.getReviewedBy() == null
                        ? null : analysis.getReviewedBy().getId(),
                analysis.getReviewedAt(),
                analysis.getCreatedAt()
        );
    }
}
