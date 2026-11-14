package com.smartresolve.repository;

import com.smartresolve.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository
        extends JpaRepository<ChatConversation, UUID> {

    List<ChatConversation> findByUser_IdOrderByUpdatedAtDesc(UUID userId);

    Optional<ChatConversation> findByIdAndUser_Id(
            UUID conversationId,
            UUID userId
    );
}
