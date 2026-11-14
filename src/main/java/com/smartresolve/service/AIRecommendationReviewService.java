package com.smartresolve.service;

import com.smartresolve.dto.request.ModifyAIRecommendationRequest;
import com.smartresolve.dto.request.ReviewAIRecommendationRequest;
import com.smartresolve.dto.response.AIAnalysisResponse;
import com.smartresolve.entity.SupportTeam;
import com.smartresolve.entity.Ticket;
import com.smartresolve.entity.TicketAIAnalysis;
import com.smartresolve.entity.TicketCategory;
import com.smartresolve.entity.TicketHistory;
import com.smartresolve.entity.User;
import com.smartresolve.enums.AIRecommendationStatus;
import com.smartresolve.enums.Role;
import com.smartresolve.enums.TicketPriority;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.TicketAIAnalysisRepository;
import com.smartresolve.repository.TicketCategoryRepository;
import com.smartresolve.repository.TicketHistoryRepository;
import com.smartresolve.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIRecommendationReviewService {

    private final TicketAIAnalysisRepository analysisRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SupportTeamRepository teamRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateReviewer(User user) {
        if (user.getRole() != Role.SUPPORT_ENGINEER
                && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only support engineers and admins can review AI recommendations");
        }
        if (!user.isActive()) {
            throw new AccessDeniedException(
                    "Inactive users cannot review recommendations");
        }
    }

    private TicketAIAnalysis getAnalysis(UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "AI recommendation not found"));
    }

    private void validatePending(TicketAIAnalysis analysis) {
        if (analysis.getRecommendationStatus()
                != AIRecommendationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This AI recommendation has already been reviewed");
        }
    }

    private void validateActiveTeam(SupportTeam team) {
        if (!team.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected support team is inactive");
        }
    }

    private void validateCategoryTeam(
            TicketCategory category,
            SupportTeam team
    ) {
        if (!category.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected category is inactive");
        }
        if (category.getSupportTeam() == null
                || !category.getSupportTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected team does not belong to the selected category");
        }
    }

    private TicketCategory getCategoryByName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI did not suggest a valid category. Use MODIFY instead.");
        }
        TicketCategory category = categoryRepository
                .findByCategoryName(name.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "AI suggested category does not exist. Use MODIFY instead."));
        if (!category.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI suggested category is inactive. Use MODIFY instead.");
        }
        return category;
    }

    private SupportTeam getTeamByName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI did not suggest a valid team. Use MODIFY instead.");
        }
        SupportTeam team = teamRepository.findByTeamName(name.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "AI suggested team does not exist. Use MODIFY instead."));
        validateActiveTeam(team);
        return team;
    }

    private void validateExistingAssignment(Ticket ticket, SupportTeam newTeam) {
        User assignedEngineer = ticket.getAssignedEngineer();
        if (assignedEngineer == null) {
            return;
        }
        if (assignedEngineer.getTeam() == null
                || !assignedEngineer.getTeam().getId().equals(newTeam.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The currently assigned engineer does not belong to the selected team. Reassign the ticket first.");
        }
    }

    private void saveHistory(
            Ticket ticket,
            User reviewer,
            String action,
            String fieldName,
            String oldValue,
            String newValue,
            String remarks
    ) {
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .performedBy(reviewer)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .remarks(remarks)
                .createdAt(LocalDateTime.now())
                .build();
        historyRepository.save(history);
    }

    private void applyRecommendation(
            TicketAIAnalysis analysis,
            TicketCategory category,
            SupportTeam team,
            TicketPriority priority,
            User reviewer,
            String remarks
    ) {
        Ticket ticket = analysis.getTicket();
        validateExistingAssignment(ticket, team);

        String oldCategory = ticket.getCategory() == null
                ? null : ticket.getCategory().getCategoryName();
        String oldPriority = ticket.getPriority() == null
                ? null : ticket.getPriority().name();
        String oldTeam = ticket.getTeam() == null
                ? null : ticket.getTeam().getTeamName();

        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setTeam(team);
        ticket.setUpdatedAt(LocalDateTime.now());

        saveHistory(ticket, reviewer, "AI_RECOMMENDATION_APPLIED",
                "category", oldCategory, category.getCategoryName(), remarks);
        saveHistory(ticket, reviewer, "AI_RECOMMENDATION_APPLIED",
                "priority", oldPriority, priority.name(), remarks);
        saveHistory(ticket, reviewer, "AI_RECOMMENDATION_APPLIED",
                "team", oldTeam, team.getTeamName(), remarks);
    }

    private void saveReviewDecision(
            TicketAIAnalysis analysis,
            User reviewer,
            AIRecommendationStatus newStatus,
            String remarks
    ) {
        saveHistory(
                analysis.getTicket(),
                reviewer,
                "AI_RECOMMENDATION_" + newStatus.name(),
                "aiRecommendation",
                AIRecommendationStatus.PENDING.name(),
                newStatus.name(),
                remarks
        );
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

    public AIAnalysisResponse acceptRecommendation(
            UUID analysisId,
            ReviewAIRecommendationRequest request,
            String email
    ) {
        User reviewer = getCurrentUser(email);
        validateReviewer(reviewer);
        TicketAIAnalysis analysis = getAnalysis(analysisId);
        validatePending(analysis);

        TicketCategory category = getCategoryByName(
                analysis.getSuggestedCategory());
        SupportTeam team = getTeamByName(analysis.getSuggestedTeam());
        validateCategoryTeam(category, team);

        if (analysis.getSuggestedPriority() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI recommendation has no valid priority");
        }

        String remarks = request == null ? null : request.remarks();
        applyRecommendation(
                analysis,
                category,
                team,
                analysis.getSuggestedPriority(),
                reviewer,
                remarks
        );
        analysis.setRecommendationStatus(AIRecommendationStatus.ACCEPTED);
        analysis.setReviewedBy(reviewer);
        analysis.setReviewedAt(LocalDateTime.now());
        saveReviewDecision(
                analysis, reviewer, AIRecommendationStatus.ACCEPTED, remarks);

        TicketAIAnalysis saved = analysisRepository.save(analysis);
        log.info("AI recommendation {} accepted by {}", analysisId, email);
        return toResponse(saved);
    }

    public AIAnalysisResponse modifyRecommendation(
            UUID analysisId,
            ModifyAIRecommendationRequest request,
            String email
    ) {
        User reviewer = getCurrentUser(email);
        validateReviewer(reviewer);
        TicketAIAnalysis analysis = getAnalysis(analysisId);
        validatePending(analysis);

        TicketCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));
        SupportTeam team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Support team not found"));
        validateActiveTeam(team);
        validateCategoryTeam(category, team);
        applyRecommendation(
                analysis, category, team, request.priority(), reviewer,
                request.reviewRemarks());

        analysis.setSummary(request.summary());
        analysis.setSuggestedCategory(category.getCategoryName());
        analysis.setSuggestedPriority(request.priority());
        analysis.setSuggestedTeam(team.getTeamName());
        analysis.setTroubleshootingSteps(request.troubleshootingSteps());
        analysis.setSuggestedResponse(request.suggestedResponse());
        analysis.setRecommendationStatus(AIRecommendationStatus.MODIFIED);
        analysis.setReviewedBy(reviewer);
        analysis.setReviewedAt(LocalDateTime.now());
        saveReviewDecision(
                analysis,
                reviewer,
                AIRecommendationStatus.MODIFIED,
                request.reviewRemarks()
        );

        TicketAIAnalysis saved = analysisRepository.save(analysis);
        log.info("AI recommendation {} modified by {}", analysisId, email);
        return toResponse(saved);
    }

    public AIAnalysisResponse rejectRecommendation(
            UUID analysisId,
            ReviewAIRecommendationRequest request,
            String email
    ) {
        User reviewer = getCurrentUser(email);
        validateReviewer(reviewer);
        TicketAIAnalysis analysis = getAnalysis(analysisId);
        validatePending(analysis);
        String remarks = request == null ? null : request.remarks();

        analysis.setRecommendationStatus(AIRecommendationStatus.REJECTED);
        analysis.setReviewedBy(reviewer);
        analysis.setReviewedAt(LocalDateTime.now());
        saveReviewDecision(
                analysis, reviewer, AIRecommendationStatus.REJECTED, remarks);

        TicketAIAnalysis saved = analysisRepository.save(analysis);
        log.info("AI recommendation {} rejected by {}", analysisId, email);
        return toResponse(saved);
    }
}
