package com.pnc.masters.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {

    Optional<ChatParticipant> findByIdConversationIdAndIdUserId(Long conversationId, Long userId);

    boolean existsByIdConversationIdAndIdUserId(Long conversationId, Long userId);
}
