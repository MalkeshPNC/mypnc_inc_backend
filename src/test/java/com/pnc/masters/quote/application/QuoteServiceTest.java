package com.pnc.masters.quote.application;

import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteHistory;
import com.pnc.masters.quote.QuoteHistoryRepository;
import com.pnc.masters.quote.QuoteLock;
import com.pnc.masters.quote.QuoteLockProperties;
import com.pnc.masters.quote.QuoteLockRepository;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteLockedException;
import com.pnc.masters.quote.api.QuoteNumberExistsException;
import com.pnc.masters.quote.api.QuoteQuantityRequest;
import com.pnc.masters.quote.api.QuoteRequest;
import com.pnc.masters.quote.api.QuoteResponse;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The lock service is wired in for real over mocked repositories: Mockito's
 * inline mock maker cannot instrument concrete classes on this JDK, and the rest
 * of the suite only ever mocks interfaces.
 */
@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private QuoteHistoryRepository historyRepository;

    @Mock
    private QuoteLockRepository lockRepository;

    @Mock
    private AppUserRepository userRepository;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        QuoteLockProperties properties = new QuoteLockProperties();
        properties.setIdleTimeoutMs(300_000L);
        QuoteLockService lockService = new QuoteLockService(lockRepository, userRepository, properties);
        quoteService = new QuoteService(quoteRepository, historyRepository, lockService, userRepository);
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Ada Lovelace")));
        lenient().when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "Grace Hopper")));
    }

    @Test
    void createUppercasesNumberAndStampsQuotePersonsWithTheSavingUser() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);
        stubSaveAssigningId();
        stubEmptyHistory();

        QuoteResponse response = quoteService.create(request(" 2026nc-100 ", null), 7L);

        assertThat(response.quoteNumber()).isEqualTo("2026NC-100");
        assertThat(response.assyQuotePerson()).isEqualTo("Ada Lovelace");
        assertThat(response.pcbQuotePerson()).isEqualTo("Ada Lovelace");
    }

    @Test
    void createRejectsDuplicateNumber() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(true);

        assertThatThrownBy(() -> quoteService.create(request("2026NC-100", null), 7L))
                .isInstanceOf(QuoteNumberExistsException.class);

        verify(quoteRepository, never()).save(any(Quote.class));
    }

    @Test
    void createRejectsMissingQuoteNumber() {
        assertThatThrownBy(() -> quoteService.create(request("  ", null), 7L))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessageContaining("quoteNumber");

        verify(quoteRepository, never()).save(any(Quote.class));
    }

    @Test
    void createRejectsMissingCreateDate() {
        QuoteRequest withoutDate = new QuoteRequest(
                "2026NC-100", null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                false, false, false, false, false, false, null, null, null, null
        );

        assertThatThrownBy(() -> quoteService.create(withoutDate, 7L))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessageContaining("createDate");
    }

    @Test
    void createAppendsCreatedHistoryRow() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);
        stubSaveAssigningId();
        stubEmptyHistory();

        quoteService.create(request("2026NC-100", null), 7L);

        QuoteHistory entry = capturedHistory();
        assertThat(entry.getAction()).isEqualTo(QuoteHistory.ACTION_CREATED);
        assertThat(entry.getQid()).isEqualTo(12L);
        assertThat(entry.getChangedByUserId()).isEqualTo(7L);
        assertThat(entry.getChangedByName()).isEqualTo("Ada Lovelace");
        assertThat(entry.getStatus()).isEqualTo("Open");
    }

    @Test
    void updateSucceedsWhileTheCallerHoldsTheLock() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        QuoteResponse response = quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        assertThat(response.lock().heldByCurrentUser()).isTrue();
        assertThat(existing.getUpdatedByUserId()).isEqualTo(7L);
    }

    @Test
    void updateRejectsWhenAnotherUserHoldsTheLock() {
        Quote existing = existingQuote();
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existing));
        holdLock(8L, LocalDateTime.now());

        assertThatThrownBy(() -> quoteService.update(12L, request("2026NC-100", List.of()), 7L))
                .isInstanceOf(QuoteLockedException.class)
                .hasMessageContaining("Grace Hopper");

        verify(quoteRepository, never()).save(any(Quote.class));
    }

    @Test
    void updateRejectsWhenTheCallersOwnLockWentIdle() {
        Quote existing = existingQuote();
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existing));
        holdLock(7L, LocalDateTime.now().minusMinutes(10));

        assertThatThrownBy(() -> quoteService.update(12L, request("2026NC-100", List.of()), 7L))
                .isInstanceOf(QuoteLockedException.class)
                .hasMessageContaining("expired");

        verify(quoteRepository, never()).save(any(Quote.class));
    }

    @Test
    void updateInsertsNewQuantityRowsAndUpdatesExistingOnes() {
        Quote existing = existingQuote();
        QuoteQuantity saved = new QuoteQuantity();
        saved.setQtyId(5L);
        saved.setQty(10);
        existing.addQuantity(saved);
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, request("2026NC-100", List.of(
                quantity(5L, 25),
                quantity(0L, 50)
        )), 7L);

        assertThat(existing.getQuantities()).hasSize(2);
        assertThat(existing.getQuantities().get(0).getQtyId()).isEqualTo(5L);
        assertThat(existing.getQuantities().get(0).getQty()).isEqualTo(25);
        assertThat(existing.getQuantities().get(1).getQtyId()).isNull();
        assertThat(existing.getQuantities().get(1).getQty()).isEqualTo(50);
    }

    @Test
    void updateDropsQuantityRowsMissingFromThePayload() {
        Quote existing = existingQuote();
        QuoteQuantity kept = new QuoteQuantity();
        kept.setQtyId(5L);
        QuoteQuantity removed = new QuoteQuantity();
        removed.setQtyId(6L);
        existing.addQuantity(kept);
        existing.addQuantity(removed);
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, request("2026NC-100", List.of(quantity(5L, 25))), 7L);

        assertThat(existing.getQuantities()).containsExactly(kept);
    }

    @Test
    void updateLetsAnAdminMoveTheCreateDate() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin()));

        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        assertThat(existing.getCreateDate()).isEqualTo(LocalDate.of(2026, 1, 5));
    }

    @Test
    void updateKeepsTheStoredCreateDateForNonAdmins() {
        Quote existing = existingQuote();
        existing.setCreateDate(LocalDate.of(2025, 11, 20));
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        assertThat(existing.getCreateDate()).isEqualTo(LocalDate.of(2025, 11, 20));
    }

    @Test
    void updateStampsReceivedDateWhenStatusBecomesReceived() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, withStatus("Received", null), 7L);

        assertThat(existing.getReceivedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void updateKeepsAnExplicitReceivedDate() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());
        LocalDate received = LocalDate.of(2026, 2, 3);

        quoteService.update(12L, withStatus("received", received), 7L);

        assertThat(existing.getReceivedDate()).isEqualTo(received);
    }

    @Test
    void updateLeavesReceivedDateEmptyForOtherStatuses() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, withStatus("Open", null), 7L);

        assertThat(existing.getReceivedDate()).isNull();
    }

    @Test
    void updateAppendsUpdatedHistoryRow() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        QuoteHistory entry = capturedHistory();
        assertThat(entry.getAction()).isEqualTo(QuoteHistory.ACTION_UPDATED);
        assertThat(entry.getAssyQuoteStatus()).isEqualTo("In Progress");
        assertThat(entry.getChangedByName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void deleteMarksQuoteDeletedAndReleasesTheLock() {
        Quote existing = existingQuote();
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existing));
        QuoteLock lock = holdLock(8L, LocalDateTime.now());

        quoteService.delete(12L);

        assertThat(existing.isDeleted()).isTrue();
        verify(quoteRepository, never()).delete(any(Quote.class));
        verify(lockRepository).delete(lock);
    }

    @Test
    void findAllReturnsOnlyActiveQuotes() {
        Quote existing = existingQuote();
        when(quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(existing));
        when(lockRepository.findAllByQidIn(List.of(12L))).thenReturn(List.of());

        var summaries = quoteService.findAll(7L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).qid()).isEqualTo(12L);
        verify(quoteRepository, never()).findAll();
    }

    @Test
    void findAllDecoratesQuotesWithTheirCurrentLockHolder() {
        Quote existing = existingQuote();
        when(quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(existing));
        when(lockRepository.findAllByQidIn(List.of(12L)))
                .thenReturn(List.of(lock(8L, LocalDateTime.now())));

        var summaries = quoteService.findAll(7L);

        assertThat(summaries.get(0).lock().lockedByName()).isEqualTo("Grace Hopper");
        assertThat(summaries.get(0).lock().heldByCurrentUser()).isFalse();
    }

    private void stubUpdate(Quote existing) {
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existing));
        when(quoteRepository.existsByQuoteNumberIgnoreCaseAndQidNot("2026NC-100", 12L)).thenReturn(false);
        when(quoteRepository.save(existing)).thenReturn(existing);
        stubEmptyHistory();
    }

    private void stubSaveAssigningId() {
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote saved = invocation.getArgument(0);
            saved.setQid(12L);
            return saved;
        });
    }

    private void stubEmptyHistory() {
        when(historyRepository.findAllByQidOrderByChangedAtDesc(12L)).thenReturn(List.of());
    }

    private QuoteHistory capturedHistory() {
        ArgumentCaptor<QuoteHistory> captor = ArgumentCaptor.forClass(QuoteHistory.class);
        verify(historyRepository).save(captor.capture());
        return captor.getValue();
    }

    private QuoteLock holdLock(Long userId, LocalDateTime lastSeenAt) {
        QuoteLock held = lock(userId, lastSeenAt);
        when(lockRepository.findByQid(12L)).thenReturn(Optional.of(held));
        return held;
    }

    private static QuoteLock lock(Long userId, LocalDateTime lastSeenAt) {
        QuoteLock lock = new QuoteLock();
        lock.setQid(12L);
        lock.setLockedByUserId(userId);
        lock.setAcquiredAt(lastSeenAt);
        lock.setLastSeenAt(lastSeenAt);
        return lock;
    }

    private static Quote existingQuote() {
        Quote quote = new Quote();
        quote.setQid(12L);
        quote.setQuoteNumber("2026NC-100");
        quote.setCreateDate(LocalDate.of(2026, 1, 5));
        return quote;
    }

    private static AppUser user(Long id, String displayName) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setDisplayName(displayName);
        return user;
    }

    private static AppUser admin() {
        AppUser user = user(7L, "Ada Lovelace");
        Role role = new Role();
        role.setRoleCode("ADMIN");
        user.setRoles(Set.of(role));
        return user;
    }

    private static QuoteQuantityRequest quantity(Long qtyId, Integer qty) {
        return new QuoteQuantityRequest(
                qtyId, "5", 5, 5, qty,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ONE, BigDecimal.ONE, "Service", "10", BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN,
                "comment", false
        );
    }

    private static QuoteRequest request(String quoteNumber, List<QuoteQuantityRequest> quantities) {
        return request(quoteNumber, quantities, "Open", null);
    }

    private static QuoteRequest withStatus(String status, LocalDate receivedDate) {
        return request("2026NC-100", List.of(), status, receivedDate);
    }

    private static QuoteRequest request(
            String quoteNumber,
            List<QuoteQuantityRequest> quantities,
            String status,
            LocalDate receivedDate
    ) {
        return new QuoteRequest(
                quoteNumber,
                "Standard",
                "PRJ-1",
                LocalDate.of(2026, 1, 5),
                null,
                3L,
                "Acme Inc",
                4L,
                "Jane Doe",
                "RFQ-1",
                5L,
                "NC-100",
                "ASSY-1",
                "PCB-1",
                "In Progress",
                "PQ-1",
                "In Progress",
                "2x2",
                new BigDecimal("5.00"),
                "note 1",
                "note 2",
                "customer note",
                "None",
                receivedDate,
                false, false, false, false, false, false,
                "Plant A",
                "USA",
                status,
                quantities
        );
    }
}
