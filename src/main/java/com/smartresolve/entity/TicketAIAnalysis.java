package com.smartresolve.entity;


import com.smartresolve.enums.AIRecommendationStatus;
import com.smartresolve.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "ticket_ai_analysis",
    indexes = {
        @Index(name = "idx_ai_ticket", columnList = "ticket_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAIAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(length = 100)
    private String suggestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TicketPriority suggestedPriority;

    @Column(length = 100)
    private String suggestedTeam;

    @Column(columnDefinition = "TEXT")
    private String troubleshootingSteps;

    @Column(columnDefinition = "TEXT")
    private String suggestedResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AIRecommendationStatus recommendationStatus =
            AIRecommendationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}