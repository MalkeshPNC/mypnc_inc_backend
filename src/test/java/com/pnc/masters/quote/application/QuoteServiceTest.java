package com.pnc.masters.quote.application;

import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.ncmaster.NcMaster;
import com.pnc.masters.ncmaster.NcMasterRepository;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteHistory;
import com.pnc.masters.quote.QuoteHistoryRepository;
import com.pnc.masters.quote.QuoteLock;
import com.pnc.masters.quote.QuoteLockProperties;
import com.pnc.masters.quote.QuoteLockRepository;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteLockedException;
import com.pnc.masters.quote.api.QuoteNotFoundException;
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
import static org.mockito.Mockito.times;
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

    @Mock
    private NcMasterRepository ncMasterRepository;

    @Mock
    private ContactRepository contactRepository;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        QuoteLockProperties properties = new QuoteLockProperties();
        properties.setIdleTimeoutMs(300_000L);
        QuoteLockService lockService = new QuoteLockService(lockRepository, userRepository, properties);
        quoteService = new QuoteService(
                quoteRepository, historyRepository, lockService, userRepository, ncMasterRepository, contactRepository);
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Ada Lovelace")));
        lenient().when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "Grace Hopper")));
        lenient().when(ncMasterRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(contactRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(quoteRepository.findFamilyQuoteNumbers(any())).thenReturn(List.of());
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
    void createFillsBlankRevisionsFromTheNc() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);
        stubSaveAssigningId();
        stubEmptyHistory();
        when(ncMasterRepository.findById(5L)).thenReturn(Optional.of(ncMaster()));

        QuoteResponse response = quoteService.create(request(" 2026nc-100 ", null), 7L);

        assertThat(response.pcbaRevision()).isEqualTo("C");
        assertThat(response.pcbRevision()).isEqualTo("B");
    }

    @Test
    void findByIdReadsDifferingPartsAndRevisionsFromTheNcBeforeTheQuoteIsUpdated() {
        Quote existing = existingQuote();
        existing.setNcId(5L);
        existing.setAssyNumber("Quote-ASSY");
        existing.setPcbNumber("Quote-PCB");
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existing));
        stubEmptyHistory();
        when(ncMasterRepository.findById(5L)).thenReturn(Optional.of(ncMaster()));

        QuoteResponse response = quoteService.findById(12L, 7L);

        assertThat(response.assyNumber()).isEqualTo("NC-ASSY");
        assertThat(response.pcbNumber()).isEqualTo("NC-PCB");
        assertThat(response.pcbaRevision()).isEqualTo("C");
        assertThat(response.pcbRevision()).isEqualTo("B");
    }

    @Test
    void findByIdKeepsQuoteOwnedFieldsAfterTheFirstUpdate() {
        Quote existing = existingQuote();
        existing.setNcId(5L);
        existing.setAssyNumber("Quote-ASSY");
        existing.setPcbNumber("Quote-PCB");
        existing.setPcbaRevision("Q1");
        existing.setPcbRevision("Q2");
        existing.setUpdatedAt(LocalDateTime.now());
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existing));
        stubEmptyHistory();

        QuoteResponse response = quoteService.findById(12L, 7L);

        assertThat(response.assyNumber()).isEqualTo("Quote-ASSY");
        assertThat(response.pcbNumber()).isEqualTo("Quote-PCB");
        assertThat(response.pcbaRevision()).isEqualTo("Q1");
        assertThat(response.pcbRevision()).isEqualTo("Q2");
    }

    @Test
    void nextCopyNumberUsesFamilyDotTwoWhenNoSuffixExists() {
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existingQuote()));
        when(quoteRepository.findFamilyQuoteNumbers("2026NC-100")).thenReturn(List.of("2026NC-100"));

        assertThat(quoteService.nextCopyNumber(12L).quoteNumber()).isEqualTo("2026NC-100.2");
    }

    @Test
    void nextCopyNumberIncrementsPastAnExistingDotTwo() {
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existingQuote()));
        when(quoteRepository.findFamilyQuoteNumbers("2026NC-100"))
                .thenReturn(List.of("2026NC-100", "2026NC-100.2"));

        assertThat(quoteService.nextCopyNumber(12L).quoteNumber()).isEqualTo("2026NC-100.3");
    }

    @Test
    void nextCopyNumberUsesTheFamilyMaxWhenCopyingARevision() {
        Quote source = existingQuote();
        source.setQuoteNumber("2026NC-100.2");
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(source));
        when(quoteRepository.findFamilyQuoteNumbers("2026NC-100"))
                .thenReturn(List.of("2026NC-100", "2026NC-100.2", "2026NC-100.3"));

        assertThat(quoteService.nextCopyNumber(12L).quoteNumber()).isEqualTo("2026NC-100.4");
    }

    @Test
    void nextCopyNumberSkipsGapsInTheSuffix() {
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(existingQuote()));
        when(quoteRepository.findFamilyQuoteNumbers("2026NC-100"))
                .thenReturn(List.of("2026NC-100", "2026NC-100.2", "2026NC-100.4"));

        assertThat(quoteService.nextCopyNumber(12L).quoteNumber()).isEqualTo("2026NC-100.5");
    }

    @Test
    void nextCopyNumberRejectsAMissingQuote() {
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.nextCopyNumber(12L))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void findFamilyOrdersOriginalThenNumericSuffixes() {
        Quote original = existingQuote();
        Quote second = quoteAt(13L, "2026NC-100.2");
        Quote third = quoteAt(14L, "2026NC-100.3");
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(original));
        when(quoteRepository.findFamilyQuotes("2026NC-100")).thenReturn(List.of(third, original, second));

        var family = quoteService.findFamily(12L, 7L);

        assertThat(family.family()).isEqualTo("2026NC-100");
        assertThat(family.quotes()).extracting(QuoteResponse::quoteNumber)
                .containsExactly("2026NC-100", "2026NC-100.2", "2026NC-100.3");
        assertThat(family.quotes()).allMatch(quote -> quote.history().isEmpty());
        assertThat(family.quotes()).allMatch(quote -> quote.familySize() == 3);
    }

    @Test
    void findFamilyFromARevisionStillReturnsTheWholeFamily() {
        Quote original = existingQuote();
        Quote second = quoteAt(13L, "2026NC-100.2");
        Quote third = quoteAt(14L, "2026NC-100.3");
        when(quoteRepository.findByQidAndIsDeletedFalse(13L)).thenReturn(Optional.of(second));
        when(quoteRepository.findFamilyQuotes("2026NC-100")).thenReturn(List.of(original, second, third));

        var family = quoteService.findFamily(13L, 7L);

        assertThat(family.quotes()).extracting(QuoteResponse::quoteNumber)
                .containsExactly("2026NC-100", "2026NC-100.2", "2026NC-100.3");
    }

    @Test
    void findFamilyOmitsQuotesTheRepositoryDoesNotReturn() {
        Quote original = existingQuote();
        Quote second = quoteAt(13L, "2026NC-100.2");
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.of(original));
        when(quoteRepository.findFamilyQuotes("2026NC-100")).thenReturn(List.of(original, second));

        var family = quoteService.findFamily(12L, 7L);

        assertThat(family.quotes()).extracting(QuoteResponse::quoteNumber)
                .containsExactly("2026NC-100", "2026NC-100.2");
        assertThat(family.quotes()).extracting(QuoteResponse::qid).doesNotContain(99L);
    }

    @Test
    void findFamilyRejectsAMissingQuote() {
        when(quoteRepository.findByQidAndIsDeletedFalse(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.findFamily(12L, 7L))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void findAllSetsFamilySizeFromSharedQuoteNumbers() {
        Quote original = existingQuote();
        Quote copy = quoteAt(13L, "2026NC-100.2");
        Quote other = quoteAt(20L, "2026NC-200");
        when(quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(original, copy, other));
        when(lockRepository.findAllByQidIn(List.of(12L, 13L, 20L))).thenReturn(List.of());

        var summaries = quoteService.findAll(7L);

        assertThat(summaries).extracting(summary -> summary.familySize())
                .containsExactly(2, 2, 1);
    }

    @Test
    void createPersistsQuantityRowsFromTheRequest() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);
        stubSaveAssigningId();
        stubEmptyHistory();

        QuoteResponse response = quoteService.create(
                request("2026NC-100", List.of(quantity(5L, 10), quantity(0L, 50))), 7L);

        assertThat(response.quantities()).hasSize(2);
        assertThat(response.quantities().get(0).qty()).isEqualTo(10);
        assertThat(response.quantities().get(1).qty()).isEqualTo(50);
        assertThat(response.quantities()).allMatch(row -> row.qtyId() == null);
    }

    @Test
    void createStoresPostedFaiValuesWhenRequiredIsOff() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);
        stubSaveAssigningId();
        stubEmptyHistory();

        QuoteResponse response = quoteService.create(withFai(
                false, "300.00", "210.00", "PNC heading",
                false, "450.00", "315.00", "AS heading"
        ), 7L);

        assertThat(response.faiReqPnc()).isFalse();
        assertThat(response.faiPcbWo()).isEqualByComparingTo("300.00");
        assertThat(response.faiPcbaWo()).isEqualByComparingTo("210.00");
        assertThat(response.faiHeadingPnc()).isEqualTo("PNC heading");
        assertThat(response.faiReqAs9102()).isFalse();
        assertThat(response.faiPcbWith()).isEqualByComparingTo("450.00");
        assertThat(response.faiPcbaWith()).isEqualByComparingTo("315.00");
        assertThat(response.faiHeadingAs9102()).isEqualTo("AS heading");
    }

    @Test
    void createRejectsMissingFaiPriceWhenRequired() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);

        assertThatThrownBy(() -> quoteService.create(withFai(
                true, null, "210.00", "PNC heading",
                false, null, null, null
        ), 7L))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessageContaining("PNC FAI PCB");

        verify(quoteRepository, never()).save(any(Quote.class));
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
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, false, false, false, false, false, false, null, null, null,
                false, null, null, null, false, null, null, null, null
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
    void updateStampsOnlyThePersonWhoseStatusChanged() {
        Quote existing = existingQuote();
        existing.setAssyQuoteStatus("In Progress");
        existing.setPcbQuoteStatus("In Progress");
        existing.setAssyQuotePerson("Old Assy");
        existing.setPcbQuotePerson("Old Pcb");
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, requestWithStatuses("Working", "In Progress"), 7L);

        assertThat(existing.getAssyQuotePerson()).isEqualTo("Ada Lovelace");
        assertThat(existing.getPcbQuotePerson()).isEqualTo("Old Pcb");
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
    void updateDiscountsLaborAndMarksUpPartsInTheSubTotal() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        // parts 100 +10% = 110, labor 200 -25% = 150, plus pcb 10 and service 5.
        quoteService.update(12L, withCommission(null,
                calcRow(2, "10", "100", "200", "25", "10", "5", "20")), 7L);

        QuoteQuantity row = existing.getQuantities().get(0);
        assertThat(row.getSubTotal()).isEqualByComparingTo("275.00");
        assertThat(row.getTotalSalesPct()).isEqualByComparingTo("275.00");
        // 275 x 2 units, then the one NRE charge.
        assertThat(row.getTotal()).isEqualByComparingTo("570.00");
    }

    @Test
    void updateAddsTheCommissionOnTopOfTheSubTotal() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, withCommission("10",
                calcRow(3, "100", null, null, null, null, null, null)), 7L);

        QuoteQuantity row = existing.getQuantities().get(0);
        assertThat(row.getSubTotal()).isEqualByComparingTo("100.00");
        assertThat(row.getTotalSalesPct()).isEqualByComparingTo("110.00");
        assertThat(row.getTotal()).isEqualByComparingTo("330.00");
    }

    @Test
    void updateRoundsDerivedAmountsHalfUpToTwoDecimals() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        // 3 x 1.085 = 3.255, which has to land on 3.26 rather than 3.25.
        quoteService.update(12L, withCommission(null,
                calcRow(1, null, "3", null, null, "8.5", null, null)), 7L);

        assertThat(existing.getQuantities().get(0).getSubTotal()).isEqualByComparingTo("3.26");
    }

    @Test
    void updateTreatsEmptyAmountsAsZero() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, withCommission(null,
                calcRow(null, null, null, null, null, null, null, null)), 7L);

        QuoteQuantity row = existing.getQuantities().get(0);
        assertThat(row.getSubTotal()).isEqualByComparingTo("0.00");
        assertThat(row.getTotalSalesPct()).isEqualByComparingTo("0.00");
        assertThat(row.getTotal()).isEqualByComparingTo("0.00");
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
    void updateStampsSubmitDateWhenStatusBecomesSubmitted() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, withSubmitted(null), 7L);

        assertThat(existing.getSubmitDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void updateKeepsAnExplicitSubmitDate() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());
        LocalDate submitted = LocalDate.of(2026, 2, 3);

        quoteService.update(12L, withSubmitted(submitted), 7L);

        assertThat(existing.getSubmitDate()).isEqualTo(submitted);
    }

    @Test
    void updateLeavesSubmitDateEmptyForOtherStatuses() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, withStatus("Open", null), 7L);

        assertThat(existing.getSubmitDate()).isNull();
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
    void updateNamesTheMainTableWhenOnlyHeaderFieldsMove() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        assertThat(capturedHistory().getChangeSummary()).isEqualTo("Changes in Main table");
    }

    @Test
    void updateNamesTheQuantityTableWhenOnlyRowsMove() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());
        // The first save settles the header, so the second can only touch rows.
        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        quoteService.update(12L, request("2026NC-100", List.of(quantity(0L, 10))), 7L);

        assertThat(lastHistory(2).getChangeSummary()).isEqualTo("Changes in Quantity table");
    }

    @Test
    void updateNamesBothTablesWhenTheHeaderAndRowsMoveTogether() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());

        quoteService.update(12L, request("2026NC-100", List.of(quantity(0L, 10))), 7L);

        assertThat(capturedHistory().getChangeSummary())
                .isEqualTo("Changes in Main table, Changes in Quantity table");
    }

    @Test
    void updateRecordsNoChangeSummaryWhenTheSaveChangesNothing() {
        Quote existing = existingQuote();
        stubUpdate(existing);
        holdLock(7L, LocalDateTime.now());
        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        quoteService.update(12L, request("2026NC-100", List.of()), 7L);

        assertThat(lastHistory(2).getChangeSummary()).isNull();
    }

    @Test
    void createRecordsNoChangeSummary() {
        when(quoteRepository.existsByQuoteNumberIgnoreCase("2026NC-100")).thenReturn(false);
        stubSaveAssigningId();
        stubEmptyHistory();

        quoteService.create(request("2026NC-100", List.of()), 7L);

        assertThat(capturedHistory().getChangeSummary()).isNull();
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
    void findAllIncludesAqAndPqStatus() {
        Quote existing = existingQuote();
        existing.setAssyQuoteStatus("Working");
        existing.setPcbQuoteStatus("Quoted");
        when(quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(existing));
        when(lockRepository.findAllByQidIn(List.of(12L))).thenReturn(List.of());

        var summaries = quoteService.findAll(7L);

        assertThat(summaries.get(0).assyQuoteStatus()).isEqualTo("Working");
        assertThat(summaries.get(0).pcbQuoteStatus()).isEqualTo("Quoted");
    }

    @Test
    void findAllIncludesSamsReview() {
        Quote existing = existingQuote();
        existing.setSamsReview(true);
        when(quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(existing));
        when(lockRepository.findAllByQidIn(List.of(12L))).thenReturn(List.of());

        var summaries = quoteService.findAll(7L);

        assertThat(summaries.get(0).samsReview()).isTrue();
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

    /** The newest entry, for tests that save more than once. */
    private QuoteHistory lastHistory(int saves) {
        ArgumentCaptor<QuoteHistory> captor = ArgumentCaptor.forClass(QuoteHistory.class);
        verify(historyRepository, times(saves)).save(captor.capture());
        return captor.getAllValues().get(saves - 1);
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
        return quoteAt(12L, "2026NC-100");
    }

    private static Quote quoteAt(Long qid, String quoteNumber) {
        Quote quote = new Quote();
        quote.setQid(qid);
        quote.setQuoteNumber(quoteNumber);
        quote.setCreateDate(LocalDate.of(2026, 1, 5));
        return quote;
    }

    private static NcMaster ncMaster() {
        NcMaster nc = new NcMaster();
        nc.setNcId(5L);
        nc.setNcNumber("NC-100");
        nc.setPcbaPartNumber("NC-ASSY");
        nc.setPcbPartNumber("NC-PCB");
        nc.setPcbaRev("C");
        nc.setPcbRev("B");
        return nc;
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
                BigDecimal.ONE, BigDecimal.ONE, "Service", BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "comment", false
        );
    }

    /**
     * A row holding only the amounts the calculation reads, in record order. The
     * nulls are deliberate: they double as the empty-cell case.
     */
    private static QuoteQuantityRequest calcRow(Integer qty, String pcbCost, String partsCost,
                                                String laborCost, String laborDiscount,
                                                String partMarkup, String serviceCharge,
                                                String pcbNre) {
        return new QuoteQuantityRequest(
                0L, null, null, null, qty,
                dec(pcbCost), dec(partsCost), dec(laborCost), dec(laborDiscount), dec(partMarkup),
                null, null, null, dec(serviceCharge),
                dec(pcbNre), null, null, null,
                null, false
        );
    }

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static QuoteRequest withFai(
            boolean reqPnc, String pcbWo, String pcbaWo, String headingPnc,
            boolean reqAs9102, String pcbWith, String pcbaWith, String headingAs9102
    ) {
        QuoteRequest base = request("2026NC-100", List.of());
        return new QuoteRequest(
                base.quoteNumber(), base.quoteType(), base.projectNumber(), base.createDate(),
                base.submitDate(), base.custId(), base.customerName(), base.contId(),
                base.contactName(), base.customerRfq(), base.ncId(), base.ncNumber(),
                base.assyNumber(), base.pcbaRevision(), base.pcbNumber(), base.pcbRevision(),
                base.assyQuoteStatus(), base.pcbQuoteNumber(), base.pcbQuoteStatus(),
                base.array(), base.commissionPercentage(), base.internalNote1(),
                base.internalNote2(), base.notesToCustomer(), base.otherNreCharges(),
                base.receivedDate(), base.pncNotes(), base.laborOnly(), base.partsScheduled(),
                base.feedback(), base.itarc(), base.berryc(), base.samsReview(),
                base.pcbaPlant(), base.pcbOrigin(), base.status(),
                reqPnc, dec(pcbWo), dec(pcbaWo), headingPnc,
                reqAs9102, dec(pcbWith), dec(pcbaWith), headingAs9102,
                base.quantities()
        );
    }

    private static QuoteRequest request(String quoteNumber, List<QuoteQuantityRequest> quantities) {
        return request(quoteNumber, quantities, "Open", null, null, "5.00");
    }

    private static QuoteRequest requestWithStatuses(String assyQuoteStatus, String pcbQuoteStatus) {
        QuoteRequest base = request("2026NC-100", List.of());
        return new QuoteRequest(
                base.quoteNumber(), base.quoteType(), base.projectNumber(), base.createDate(),
                base.submitDate(), base.custId(), base.customerName(), base.contId(),
                base.contactName(), base.customerRfq(), base.ncId(), base.ncNumber(),
                base.assyNumber(), base.pcbaRevision(), base.pcbNumber(), base.pcbRevision(),
                assyQuoteStatus, base.pcbQuoteNumber(),
                pcbQuoteStatus, base.array(), base.commissionPercentage(), base.internalNote1(),
                base.internalNote2(), base.notesToCustomer(), base.otherNreCharges(),
                base.receivedDate(), base.pncNotes(), base.laborOnly(), base.partsScheduled(),
                base.feedback(), base.itarc(), base.berryc(), base.samsReview(),
                base.pcbaPlant(), base.pcbOrigin(), base.status(),
                base.faiReqPnc(), base.faiPcbWo(), base.faiPcbaWo(), base.faiHeadingPnc(),
                base.faiReqAs9102(), base.faiPcbWith(), base.faiPcbaWith(), base.faiHeadingAs9102(),
                base.quantities()
        );
    }

    private static QuoteRequest withStatus(String status, LocalDate receivedDate) {
        return request("2026NC-100", List.of(), status, null, receivedDate, "5.00");
    }

    /** A submitted quote, optionally carrying a submit date already. */
    private static QuoteRequest withSubmitted(LocalDate submitDate) {
        return request("2026NC-100", List.of(), "Submitted", submitDate, null, "5.00");
    }

    /** One row and an explicit commission, for the calculation tests. */
    private static QuoteRequest withCommission(String commission, QuoteQuantityRequest row) {
        return request("2026NC-100", List.of(row), "Open", null, null, commission);
    }

    private static QuoteRequest request(
            String quoteNumber,
            List<QuoteQuantityRequest> quantities,
            String status,
            LocalDate submitDate,
            LocalDate receivedDate,
            String commission
    ) {
        return new QuoteRequest(
                quoteNumber,
                "Standard",
                "PRJ-1",
                LocalDate.of(2026, 1, 5),
                submitDate,
                3L,
                "Acme Inc",
                4L,
                "Jane Doe",
                "RFQ-1",
                5L,
                "NC-100",
                "ASSY-1",
                null,
                "PCB-1",
                null,
                "In Progress",
                "PQ-1",
                "In Progress",
                "2x2",
                dec(commission),
                "note 1",
                "note 2",
                "customer note",
                "None",
                receivedDate,
                false, false, false, false, false, false, false,
                "Plant A",
                "USA",
                status,
                false,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                quantities
        );
    }
}
