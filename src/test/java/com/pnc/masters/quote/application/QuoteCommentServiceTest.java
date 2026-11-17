package com.pnc.masters.quote.application;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteComment;
import com.pnc.masters.quote.QuoteCommentImageRepository;
import com.pnc.masters.quote.QuoteCommentMentionRepository;
import com.pnc.masters.quote.QuoteCommentRepository;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteCommentServiceTest {

    @Mock
    private QuoteRepository quotes;
    @Mock
    private QuoteCommentRepository comments;
    @Mock
    private QuoteCommentMentionRepository mentions;
    @Mock
    private QuoteCommentImageRepository images;
    @Mock
    private AppUserRepository users;
    @Mock
    private DocumentStorage storage;
    @Mock
    private AppNotifier notifier;
    @Mock
    private QuoteCommentEvents events;

    private QuoteCommentService service;

    @BeforeEach
    void setUp() {
        service = new QuoteCommentService(quotes, comments, mentions, images, users, storage, notifier, events);
    }

    @Test
    void mentionNotifiesTheTaggedPersonAndSkipsTheAuthor() {
        Quote quote = new Quote();
        quote.setQuoteNumber("2026NC-1");
        when(quotes.findByQidAndIsDeletedFalse(9L)).thenReturn(Optional.of(quote));
        when(comments.saveAndFlush(any(QuoteComment.class))).thenAnswer(invocation -> {
            QuoteComment saved = invocation.getArgument(0);
            saved.setCommentId(3L);
            return saved;
        });
        AppUser tagged = user(4L, "Paresh");
        AppUser author = user(2L, "Malkesh");
        when(users.findAllById(any())).thenReturn(List.of(tagged));
        when(users.findById(2L)).thenReturn(Optional.of(author));

        service.create(9L, 2L, "Can you check this?", null, List.of(2L, 4L), List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> recipients = ArgumentCaptor.forClass(List.class);
        verify(notifier).notifyUsers(recipients.capture(), eq("QUOTE_COMMENT"),
                eq("Malkesh mentioned you on 2026NC-1"), eq("Can you check this?"),
                eq("/quote-systems/quotes/9"));
        assertThat(recipients.getValue()).containsExactly(4L);
    }

    private static AppUser user(Long id, String name) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setDisplayName(name);
        user.setEnabled(true);
        return user;
    }
}
