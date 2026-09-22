package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatConversationNotFoundException;
import com.pnc.masters.chat.api.ChatConversationResponse;
import com.pnc.masters.chat.api.ChatCreateConversationRequest;
import com.pnc.masters.chat.api.ChatForbiddenException;
import com.pnc.masters.chat.api.ChatMessageResponse;
import com.pnc.masters.chat.api.ChatMessageValidationException;
import com.pnc.masters.chat.api.ChatPeerNotFoundException;
import com.pnc.masters.chat.api.ChatPersonResponse;
import com.pnc.masters.chat.api.ChatSendMessageRequest;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {

    static final int MESSAGE_PAGE = 50;

    private final AppUserRepository users;
    private final ChatConversationRepository conversations;
    private final ChatParticipantRepository participants;
    private final ChatMessageRepository messages;
    private final ChatPresenceRegistry presence;
    private final ChatSendRateLimiter rateLimiter;
    private final ChatMessagePublisher publisher;

    public ChatService(
            AppUserRepository users,
            ChatConversationRepository conversations,
            ChatParticipantRepository participants,
            ChatMessageRepository messages,
            ChatPresenceRegistry presence,
            ChatSendRateLimiter rateLimiter,
            ChatMessagePublisher publisher
    ) {
        this.users = users;
        this.conversations = conversations;
        this.participants = participants;
        this.messages = messages;
        this.presence = presence;
        this.rateLimiter = rateLimiter;
        this.publisher = publisher;
    }

    public List<ChatPersonResponse> listPeople(Long userId) {
        return users.findByEnabledTrueAndUserIdNotOrderByDisplayNameAsc(userId).stream()
                .map(this::toPerson)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> listConversations(Long userId) {
        List<ChatConversationResponse> inbox = new ArrayList<>();
        for (ChatConversation conversation : conversations.findByUserLoOrUserHiOrderByConversationIdDesc(userId, userId)) {
            inbox.add(toConversation(conversation, userId));
        }
        inbox.sort(Comparator
                .comparing(ChatConversationResponse::unreadCount).reversed()
                .thenComparing(row -> row.lastMessage() == null ? LocalDateTime.MIN : row.lastMessage().createdAt(),
                        Comparator.reverseOrder()));
        return inbox;
    }

    @Transactional
    public ChatConversationResponse openConversation(Long userId, ChatCreateConversationRequest request) {
        Long peerId = request.peerUserId();
        if (peerId == null || peerId.equals(userId)) {
            throw new ChatMessageValidationException("Pick another person to chat with.");
        }
        requireEnabledPeer(peerId);
        long lo = Math.min(userId, peerId);
        long hi = Math.max(userId, peerId);
        ChatConversation conversation = conversations.findByUserLoAndUserHi(lo, hi)
                .orElseGet(() -> createPair(lo, hi));
        return toConversation(conversation, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(Long userId, Long conversationId, Long beforeId, Integer limit) {
        ChatConversation conversation = requireParticipant(conversationId, userId);
        int pageSize = limit == null || limit < 1 ? MESSAGE_PAGE : Math.min(limit, MESSAGE_PAGE);
        List<ChatMessage> page = beforeId == null
                ? messages.findByConversationConversationIdOrderByMessageIdDesc(
                        conversation.getConversationId(), PageRequest.of(0, pageSize))
                : messages.findByConversationConversationIdAndMessageIdLessThanOrderByMessageIdDesc(
                        conversation.getConversationId(), beforeId, PageRequest.of(0, pageSize));
        List<ChatMessageResponse> oldestFirst = new ArrayList<>(page.size());
        for (int i = page.size() - 1; i >= 0; i--) {
            oldestFirst.add(toMessage(page.get(i)));
        }
        return oldestFirst;
    }

    @Transactional
    public ChatMessageResponse send(Long userId, Long conversationId, ChatSendMessageRequest request) {
        rateLimiter.check(userId);
        String body = request.body() == null ? "" : request.body().trim();
        if (body.isEmpty()) {
            throw new ChatMessageValidationException("Message cannot be empty.");
        }
        if (body.length() > 2000) {
            throw new ChatMessageValidationException("Message is too long.");
        }
        ChatConversation conversation = requireParticipant(conversationId, userId);
        requireEnabledPeer(conversation.peerId(userId));
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderId(userId);
        message.setBody(body);
        ChatMessage saved = messages.save(message);
        markRead(userId, conversationId);
        ChatMessageResponse response = toMessage(saved);
        pushMessage(userId, conversation.peerId(userId), response);
        return response;
    }

    @Transactional
    public void markRead(Long userId, Long conversationId) {
        ChatParticipant participant = participants.findByIdConversationIdAndIdUserId(conversationId, userId)
                .orElseThrow(ChatForbiddenException::new);
        participant.setLastReadAt(LocalDateTime.now());
    }

    private ChatConversation createPair(long lo, long hi) {
        try {
            ChatConversation conversation = new ChatConversation();
            conversation.setUserLo(lo);
            conversation.setUserHi(hi);
            ChatConversation saved = conversations.saveAndFlush(conversation);
            participants.save(participant(saved, lo));
            participants.save(participant(saved, hi));
            return saved;
        } catch (DataIntegrityViolationException duplicate) {
            return conversations.findByUserLoAndUserHi(lo, hi)
                    .orElseThrow(() -> duplicate);
        }
    }

    private static ChatParticipant participant(ChatConversation conversation, Long userId) {
        ChatParticipant row = new ChatParticipant();
        row.setId(new ChatParticipantId(conversation.getConversationId(), userId));
        row.setConversation(conversation);
        return row;
    }

    private ChatConversation requireParticipant(Long conversationId, Long userId) {
        ChatConversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new ChatConversationNotFoundException(conversationId));
        if (!participants.existsByIdConversationIdAndIdUserId(conversationId, userId)) {
            throw new ChatForbiddenException();
        }
        return conversation;
    }

    private AppUser requireEnabledPeer(Long peerId) {
        AppUser peer = users.findById(peerId).orElseThrow(ChatPeerNotFoundException::new);
        if (!peer.isEnabled()) {
            throw new ChatPeerNotFoundException();
        }
        return peer;
    }

    private ChatConversationResponse toConversation(ChatConversation conversation, Long userId) {
        Long peerId = conversation.peerId(userId);
        AppUser peer = users.findById(peerId).orElse(null);
        ChatPersonResponse person = peer == null
                ? new ChatPersonResponse(peerId, "Unknown user", ChatPresenceRegistry.OFFLINE, presence.lastSeenAt(peerId))
                : toPerson(peer);
        ChatMessage last = messages.findTopByConversationConversationIdOrderByMessageIdDesc(conversation.getConversationId())
                .orElse(null);
                LocalDateTime lastRead = participants.findByIdConversationIdAndIdUserId(conversation.getConversationId(), userId)
                .map(ChatParticipant::getLastReadAt)
                .orElse(null);
        long unread = lastRead == null
                ? messages.countByConversationConversationIdAndSenderIdNot(conversation.getConversationId(), userId)
                : messages.countByConversationConversationIdAndSenderIdNotAndCreatedAtAfter(
                        conversation.getConversationId(), userId, lastRead);
        return new ChatConversationResponse(
                conversation.getConversationId(),
                person,
                last == null ? null : toMessage(last),
                unread
        );
    }

    private ChatPersonResponse toPerson(AppUser user) {
        return new ChatPersonResponse(
                user.getUserId(),
                user.getDisplayName(),
                presence.status(user.getUserId()),
                presence.lastSeenAt(user.getUserId())
        );
    }

    private static ChatMessageResponse toMessage(ChatMessage message) {
        return new ChatMessageResponse(
                message.getMessageId(),
                message.getConversation().getConversationId(),
                message.getSenderId(),
                message.getBody(),
                message.getCreatedAt()
        );
    }

    private void pushMessage(Long senderId, Long peerId, ChatMessageResponse message) {
        publisher.publish(senderId, message);
        publisher.publish(peerId, message);
    }
}
