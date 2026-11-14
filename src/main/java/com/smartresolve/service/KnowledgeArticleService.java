package com.smartresolve.service;

import com.smartresolve.dto.request.KnowledgeArticleRequest;
import com.smartresolve.dto.response.KnowledgeArticleResponse;
import com.smartresolve.entity.KnowledgeArticle;
import com.smartresolve.entity.SupportTeam;
import com.smartresolve.entity.Ticket;
import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import com.smartresolve.enums.TicketStatus;
import com.smartresolve.repository.KnowledgeArticleRepository;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeArticleService {

    private final KnowledgeArticleRepository articleRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore;

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private void validateReviewer(User user) {
        if (!user.isActive()
                || (user.getRole() != Role.SUPPORT_ENGINEER
                && user.getRole() != Role.ADMIN)) {
            throw new AccessDeniedException(
                    "Only active support engineers and admins can create verified knowledge articles"
            );
        }
    }

    private void validateSourceTicket(UUID ticketId) {
        if (ticketId == null) {
            return;
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Source ticket not found"
                ));

        if (ticket.getStatus() != TicketStatus.RESOLVED
                && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only resolved or closed tickets can be used as knowledge sources"
            );
        }

        if (ticket.getResolutionNotes() == null
                || ticket.getResolutionNotes().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source ticket must contain resolution notes"
            );
        }
    }

    @Transactional
    public KnowledgeArticleResponse createArticle(
            KnowledgeArticleRequest request,
            String email
    ) {
        User reviewer = getCurrentUser(email);
        validateReviewer(reviewer);
        validateSourceTicket(request.sourceTicketId());

        KnowledgeArticle article = KnowledgeArticle.builder()
                .title(request.title().trim())
                .content(request.content().trim())
                .sourceTicketId(request.sourceTicketId())
                .verified(true)
                .active(true)
                .verifiedBy(reviewer)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        KnowledgeArticle saved = articleRepository.save(article);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("articleId", saved.getId().toString());
        metadata.put("title", saved.getTitle());
        metadata.put("verified", true);
        metadata.put("active", true);
        if (saved.getSourceTicketId() != null) {
            metadata.put("sourceTicketId", saved.getSourceTicketId().toString());
        }

        String documentContent = """
                Knowledge Article: %s

                Verified Solution:
                %s
                """.formatted(saved.getTitle(), saved.getContent());

        Document document = Document.builder()
                .id(saved.getId().toString())
                .text(documentContent)
                .metadata(metadata)
                .build();
        vectorStore.add(List.of(document));

        log.info(
                "Knowledge article {} created and indexed by {}",
                saved.getId(),
                email
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeArticleResponse> getVerifiedArticles(String email) {
        User user = getCurrentUser(email);
        validateReviewer(user);
        return articleRepository.findByVerifiedTrueAndActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private KnowledgeArticleResponse toResponse(KnowledgeArticle article) {
        return new KnowledgeArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSourceTicketId(),
                article.isVerified(),
                article.getVerifiedBy() == null
                        ? null : article.getVerifiedBy().getFullName(),
                article.getCreatedAt()
        );
    }
}
