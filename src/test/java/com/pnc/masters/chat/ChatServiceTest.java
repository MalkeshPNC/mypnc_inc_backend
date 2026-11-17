package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatCreateConversationRequest;
import com.pnc.masters.chat.api.ChatForbiddenException;
import com.pnc.masters.chat.api.ChatMessageResponse;
import com.pnc.masters.chat.api.ChatMessageValidationException;
import com.pnc.masters.chat.api.ChatPeerNotFoundException;
import com.pnc.masters.chat.api.ChatSendMessageRequest;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Presence and the send limiter are real: Mockito cannot instrument concrete
 * classes on this JDK. Repositories and the message publisher are interfaces.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private AppUserRepository users;
    @Mock private ChatConversationRepository conversations;
    @Mock private ChatParticipantRepository participants;
    @Mock private ChatMessageRepository messages;

    private final ChatPresenceRegistry presence = new ChatPresenceRegistry();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private ChatService service;
    private AppUser ada;
    private AppUser bob;

    @BeforeEach
    void setUp() {
        ada = user(1L, "Ada Lovelace", true);
        bob = user(2L, "Bob", true);
        service = new ChatService(
                users,
                conversations,
                participants,
                messages,
                presence,
                new ChatSendRateLimiter(),
                publisher
        );
    }

    @Test
    void openConversationStoresLoHiPairAndBothParticipants() {
        when(users.findById(2L)).thenReturn(Optional.of(bob));
        when(conversations.findByUserLoAndUserHi(1L, 2L)).thenReturn(Optional.empty());
        when(conversations.saveAndFlush(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setConversationId(10L);
            return saved;
        });
        when(participants.findByIdConversationIdAndIdUserId(10L, 1L)).thenReturn(Optional.of(participant(10L, 1L)));
        when(messages.findTopByConversationConversationIdOrderByMessageIdDesc(10L)).thenReturn(Optional.empty());
        when(messages.countByConversationConversationIdAndSenderIdNot(10L, 1L)).thenReturn(0L);

        var response = service.openConversation(1L, new ChatCreateConversationRequest(2L));

        assertThat(response.conversationId()).isEqualTo(10L);
        assertThat(response.peer().userId()).isEqualTo(2L);
        ArgumentCaptor<ChatConversation> captor = ArgumentCaptor.forClass(ChatConversation.class);
        verify(conversations).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserLo()).isEqualTo(1L);
        assertThat(captor.getValue().getUserHi()).isEqualTo(2L);
        verify(participants, times(2)).save(any(ChatParticipant.class));
    }

    @Test
    void openConversationReusesTheExistingPair() {
        ChatConversation existing = conversation(10L, 1L, 2L);
        when(users.findById(1L)).thenReturn(Optional.of(ada));
        when(users.findById(2L)).thenReturn(Optional.of(bob));
        when(conversations.findByUserLoAndUserHi(1L, 2L)).thenReturn(Optional.of(existing));
        when(participants.findByIdConversationIdAndIdUserId(10L, 1L)).thenReturn(Optional.of(participant(10L, 1L)));
        when(participants.findByIdConversationIdAndIdUserId(10L, 2L)).thenReturn(Optional.of(participant(10L, 2L)));
        when(messages.findTopByConversationConversationIdOrderByMessageIdDesc(10L)).thenReturn(Optional.empty());
        when(messages.countByConversationConversationIdAndSenderIdNot(10L, 1L)).thenReturn(0L);
        when(messages.countByConversationConversationIdAndSenderIdNot(10L, 2L)).thenReturn(0L);

        var first = service.openConversation(1L, new ChatCreateConversationRequest(2L));
        var second = service.openConversation(2L, new ChatCreateConversationRequest(1L));

        assertThat(first.conversationId()).isEqualTo(10L);
        assertThat(second.conversationId()).isEqualTo(10L);
        verify(conversations, never()).saveAndFlush(any());
    }

    @Test
    void openConversationRejectsSelfAndDisabledPeer() {
        assertThatThrownBy(() -> service.openConversation(1L, new ChatCreateConversationRequest(1L)))
                .isInstanceOf(ChatMessageValidationException.class);

        AppUser disabled = user(3L, "Closed", false);
        when(users.findById(3L)).thenReturn(Optional.of(disabled));
        assertThatThrownBy(() -> service.openConversation(1L, new ChatCreateConversationRequest(3L)))
                .isInstanceOf(ChatPeerNotFoundException.class);

        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.openConversation(1L, new ChatCreateConversationRequest(99L)))
                .isInstanceOf(ChatPeerNotFoundException.class);
    }

    @Test
    void sendRejectsACallerWhoIsNotAParticipant() {
        when(conversations.findById(10L)).thenReturn(Optional.of(conversation(10L, 1L, 2L)));
        when(participants.existsByIdConversationIdAndIdUserId(10L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> service.send(9L, 10L, new ChatSendMessageRequest("hello")))
                .isInstanceOf(ChatForbiddenException.class);
        verify(messages, never()).save(any());
        assertThat(publisher.sent).isEmpty();
    }

    @Test
    void sendPersistsAndPushesToBothParticipants() {
        ChatConversation conversation = conversation(10L, 1L, 2L);
        when(conversations.findById(10L)).thenReturn(Optional.of(conversation));
        when(participants.existsByIdConversationIdAndIdUserId(10L, 1L)).thenReturn(true);
        when(users.findById(2L)).thenReturn(Optional.of(bob));
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setMessageId(44L);
            saved.setCreatedAt(LocalDateTime.parse("2026-09-22T12:00:00"));
            return saved;
        });
        when(participants.findByIdConversationIdAndIdUserId(10L, 1L)).thenReturn(Optional.of(participant(10L, 1L)));

        var response = service.send(1L, 10L, new ChatSendMessageRequest("  hello  "));

        assertThat(response.messageId()).isEqualTo(44L);
        assertThat(response.body()).isEqualTo("hello");
        assertThat(publisher.sent).extracting(sent -> sent.userId).containsExactly(1L, 2L);
    }

    @Test
    void listPeopleExcludesSelfAndUsesLivePresence() {
        presence.connect(2L, "tab-1");
        when(users.findByEnabledTrueAndUserIdNotOrderByDisplayNameAsc(1L)).thenReturn(List.of(bob));

        var people = service.listPeople(1L);

        assertThat(people).hasSize(1);
        assertThat(people.getFirst().userId()).isEqualTo(2L);
        assertThat(people.getFirst().status()).isEqualTo(ChatPresenceRegistry.ONLINE);
        assertThat(people.getFirst().displayName()).isEqualTo("Bob");
    }

    private static AppUser user(Long id, String name, boolean enabled) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setDisplayName(name);
        user.setEnabled(enabled);
        return user;
    }

    private static ChatConversation conversation(Long id, Long lo, Long hi) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(id);
        conversation.setUserLo(lo);
        conversation.setUserHi(hi);
        return conversation;
    }

    private static ChatParticipant participant(Long conversationId, Long userId) {
        ChatParticipant row = new ChatParticipant();
        row.setId(new ChatParticipantId(conversationId, userId));
        return row;
    }

    private static final class RecordingPublisher implements ChatMessagePublisher {
        private final List<Sent> sent = new ArrayList<>();

        @Override
        public void publish(Long userId, ChatMessageResponse message) {
            sent.add(new Sent(userId, message));
        }
    }

    private record Sent(Long userId, ChatMessageResponse message) {
    }
}
