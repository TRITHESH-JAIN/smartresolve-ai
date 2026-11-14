package com.smartresolve.repository;

import com.smartresolve.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeArticleRepository
        extends JpaRepository<KnowledgeArticle, UUID> {

    List<KnowledgeArticle> findByVerifiedTrueAndActiveTrue();

    List<KnowledgeArticle> findBySourceTicketId(UUID sourceTicketId);
}
