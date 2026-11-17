package com.pnc.masters.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findTopByConversationConversationIdOrderByMessageIdDesc(Long conversationId);

    List<ChatMessage> findByConversationConversationIdOrderByMessageIdDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByConversationConversationIdAndMessageIdLessThanOrderByMessageIdDesc(
            Long conversationId,
            Long beforeId,
            Pageable pageable
    );

    long countByConversationConversationIdAndSenderIdNot(Long conversationId, Long senderId);

    long countByConversationConversationIdAndSenderIdNotAndCreatedAtAfter(
            Long conversationId,
            Long senderId,
            LocalDateTime createdAt
    );
}
