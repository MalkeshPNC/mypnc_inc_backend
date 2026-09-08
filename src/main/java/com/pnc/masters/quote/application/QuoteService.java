package com.pnc.masters.quote.application;

import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteHistory;
import com.pnc.masters.quote.QuoteHistoryRepository;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteHistoryResponse;
import com.pnc.masters.quote.api.QuoteLockResponse;
import com.pnc.masters.quote.api.QuoteNotFoundException;
import com.pnc.masters.quote.api.QuoteNumberExistsException;
import com.pnc.masters.quote.api.QuoteQuantityRequest;
import com.pnc.masters.quote.api.QuoteQuantityResponse;
import com.pnc.masters.quote.api.QuoteRequest;
import com.pnc.masters.quote.api.QuoteResponse;
import com.pnc.masters.quote.api.QuoteSummaryResponse;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class QuoteService {

    private static final Set<String> CREATE_DATE_ROLES = Set.of("ADMIN");

    /** The quotestatus entry that means the quote came back from the customer. */
    private static final String RECEIVED_STATUS = "received";

    private final QuoteRepository quoteRepository;
    private final QuoteHistoryRepository historyRepository;
    private final QuoteLockService lockService;
    private final AppUserRepository userRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        QuoteHistoryRepository historyRepository,
                        QuoteLockService lockService,
                        AppUserRepository userRepository) {
        this.quoteRepository = quoteRepository;
        this.historyRepository = historyRepository;
        this.lockService = lockService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<QuoteSummaryResponse> findAll(Long userId) {
        List<Quote> quotes = quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();
        Map<Long, QuoteLockResponse> locks =
                lockService.findAll(quotes.stream().map(Quote::getQid).toList(), userId);
        return quotes.stream().map(quote -> toSummary(quote, locks.get(quote.getQid()))).toList();
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(Long id, Long userId) {
        return toResponse(getQuote(id), userId);
    }

    /**
     * Creates the header only. Quantity lines are added later from the edit
     * screen, which is the only place they exist.
     */
    public QuoteResponse create(QuoteRequest request, Long userId) {
        String quoteNumber = requireQuoteNumber(request);
        if (quoteRepository.existsByQuoteNumberIgnoreCase(quoteNumber)) {
            throw new QuoteNumberExistsException(quoteNumber);
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        Quote quote = new Quote();
        applyRequest(quote, request, quoteNumber, user);
        quote.setCreatedByUserId(userId);
        Quote saved = quoteRepository.save(quote);
        recordHistory(saved, QuoteHistory.ACTION_CREATED, userId, user);
        return toResponse(saved, userId);
    }

    public QuoteResponse update(Long id, QuoteRequest request, Long userId) {
        Quote quote = getQuote(id);
        lockService.requireHeldBy(id, userId);
        String quoteNumber = requireQuoteNumber(request);
        if (quoteRepository.existsByQuoteNumberIgnoreCaseAndQidNot(quoteNumber, id)) {
            throw new QuoteNumberExistsException(quoteNumber);
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        LocalDate createDate = quote.getCreateDate();
        applyRequest(quote, request, quoteNumber, user);
        // Create date is role-gated in the UI, so hold the line here rather than
        // trusting whatever the client posted.
        if (!mayEditCreateDate(user)) {
            quote.setCreateDate(createDate);
        }
        applyQuantities(quote, request.quantities());
        quote.setUpdatedByUserId(userId);
        quote.setUpdatedAt(LocalDateTime.now());
        Quote saved = quoteRepository.save(quote);
        recordHistory(saved, QuoteHistory.ACTION_UPDATED, userId, user);
        return toResponse(saved, userId);
    }

    public void delete(Long id) {
        Quote quote = getQuote(id);
        quote.setDeleted(true);
        quoteRepository.save(quote);
        lockService.forceRelease(id);
    }

    private Quote getQuote(Long id) {
        return quoteRepository.findByQidAndIsDeletedFalse(id).orElseThrow(() -> new QuoteNotFoundException(id));
    }

    private void applyRequest(Quote quote, QuoteRequest request, String quoteNumber, AppUser user) {
        quote.setQuoteNumber(quoteNumber);
        quote.setQuoteType(blankToNull(request.quoteType()));
        quote.setProjectNumber(blankToNull(request.projectNumber()));
        quote.setCreateDate(request.createDate());
        quote.setSubmitDate(request.submitDate());
        quote.setCustId(request.custId());
        quote.setCustomerName(blankToNull(request.customerName()));
        quote.setContId(request.contId());
        quote.setContactName(blankToNull(request.contactName()));
        quote.setCustomerRfq(blankToNull(request.customerRfq()));
        quote.setNcId(request.ncId());
        quote.setNcNumber(blankToNull(request.ncNumber()));
        quote.setAssyNumber(blankToNull(request.assyNumber()));
        quote.setPcbNumber(blankToNull(request.pcbNumber()));
        quote.setAssyQuoteStatus(blankToNull(request.assyQuoteStatus()));
        quote.setPcbQuoteNumber(blankToNull(request.pcbQuoteNumber()));
        quote.setPcbQuoteStatus(blankToNull(request.pcbQuoteStatus()));
        quote.setQuoteArray(blankToNull(request.array()));
        quote.setSalesPercentage(request.salesPercentage());
        quote.setInternalNote1(blankToNull(request.internalNote1()));
        quote.setInternalNote2(blankToNull(request.internalNote2()));
        quote.setNotesToCustomer(blankToNull(request.notesToCustomer()));
        quote.setOtherNreCharges(blankToNull(request.otherNreCharges()));
        quote.setReceivedDate(request.receivedDate());
        // A received quote always has a received date, whatever the client sent.
        if (quote.getReceivedDate() == null && isReceived(request.status())) {
            quote.setReceivedDate(LocalDate.now());
        }
        quote.setPncNotes(isTrue(request.pncNotes()));
        quote.setLaborOnly(isTrue(request.laborOnly()));
        quote.setPartsScheduled(isTrue(request.partsScheduled()));
        quote.setFeedback(isTrue(request.feedback()));
        quote.setItarc(isTrue(request.itarc()));
        quote.setBerryc(isTrue(request.berryc()));
        quote.setPcbaPlant(blankToNull(request.pcbaPlant()));
        quote.setPcbOrigin(blankToNull(request.pcbOrigin()));
        quote.setStatus(blankToNull(request.status()));
        // The quote person columns are server owned: whoever saves owns the entry.
        String personName = displayName(user);
        quote.setAssyQuotePerson(personName);
        quote.setPcbQuotePerson(personName);
    }

    /**
     * Syncs the submitted lines onto the quote: rows without an id are inserted,
     * known ids are updated in place, and anything the client left out is removed
     * through orphanRemoval.
     */
    private void applyQuantities(Quote quote, List<QuoteQuantityRequest> requested) {
        List<QuoteQuantityRequest> rows = requested == null ? List.of() : requested;
        Map<Long, QuoteQuantity> existing = new LinkedHashMap<>();
        for (QuoteQuantity quantity : quote.getQuantities()) {
            existing.put(quantity.getQtyId(), quantity);
        }

        List<Long> keptIds = rows.stream()
                .map(QuoteQuantityRequest::qtyId)
                .filter(qtyId -> qtyId != null && qtyId > 0)
                .toList();
        quote.getQuantities().removeIf(quantity -> !keptIds.contains(quantity.getQtyId()));

        for (QuoteQuantityRequest row : rows) {
            boolean isNew = row.qtyId() == null || row.qtyId() <= 0;
            QuoteQuantity quantity = isNew ? null : existing.get(row.qtyId());
            if (quantity == null) {
                quantity = new QuoteQuantity();
                applyQuantity(quantity, row);
                quote.addQuantity(quantity);
            } else {
                applyQuantity(quantity, row);
            }
        }
    }

    private void applyQuantity(QuoteQuantity quantity, QuoteQuantityRequest row) {
        quantity.setLt(blankToNull(row.lt()));
        quantity.setLtPcb(row.ltPcb());
        quantity.setLtPcba(row.ltPcba());
        quantity.setQty(row.qty());
        quantity.setPcbCost(row.pcbCost());
        quantity.setPartsCost(row.partsCost());
        quantity.setLaborCost(row.laborCost());
        quantity.setLaborDiscount(row.laborDiscount());
        quantity.setPartMarkup(row.partMarkup());
        quantity.setTesting(row.testing());
        quantity.setConfCoat(row.confCoat());
        quantity.setServiceName(blankToNull(row.serviceName()));
        quantity.setServiceCharge(blankToNull(row.serviceCharge()));
        quantity.setSubTotal(row.subTotal());
        quantity.setTotalSalesPct(row.totalSalesPct());
        quantity.setPcbNre(row.pcbNre());
        quantity.setAssyNre(row.assyNre());
        quantity.setStencil(row.stencil());
        quantity.setOtherNre(row.otherNre());
        quantity.setTotal(row.total());
        quantity.setComments(blankToNull(row.comments()));
        quantity.setReceived(isTrue(row.received()));
    }

    private void recordHistory(Quote quote, String action, Long userId, AppUser user) {
        QuoteHistory entry = new QuoteHistory();
        entry.setQid(quote.getQid());
        entry.setAction(action);
        entry.setStatus(quote.getStatus());
        entry.setAssyQuoteStatus(quote.getAssyQuoteStatus());
        entry.setPcbQuoteStatus(quote.getPcbQuoteStatus());
        entry.setChangedByUserId(userId);
        entry.setChangedByName(displayName(user));
        entry.setChangedAt(LocalDateTime.now());
        historyRepository.save(entry);
    }

    private QuoteSummaryResponse toSummary(Quote quote, QuoteLockResponse lock) {
        return new QuoteSummaryResponse(
                quote.getQid(),
                quote.getQuoteNumber(),
                quote.getQuoteType(),
                quote.getCustomerName(),
                quote.getNcNumber(),
                quote.getStatus(),
                quote.getCreateDate(),
                lock
        );
    }

    private QuoteResponse toResponse(Quote quote, Long userId) {
        return new QuoteResponse(
                quote.getQid(),
                quote.getQuoteNumber(),
                quote.getQuoteType(),
                quote.getProjectNumber(),
                quote.getCreateDate(),
                quote.getSubmitDate(),
                quote.getCustId(),
                quote.getCustomerName(),
                quote.getContId(),
                quote.getContactName(),
                quote.getCustomerRfq(),
                quote.getNcId(),
                quote.getNcNumber(),
                quote.getAssyNumber(),
                quote.getPcbNumber(),
                quote.getAssyQuoteStatus(),
                quote.getAssyQuotePerson(),
                quote.getPcbQuoteNumber(),
                quote.getPcbQuoteStatus(),
                quote.getPcbQuotePerson(),
                quote.getQuoteArray(),
                quote.getSalesPercentage(),
                quote.getInternalNote1(),
                quote.getInternalNote2(),
                quote.getNotesToCustomer(),
                quote.getOtherNreCharges(),
                quote.getReceivedDate(),
                quote.isPncNotes(),
                quote.isLaborOnly(),
                quote.isPartsScheduled(),
                quote.isFeedback(),
                quote.isItarc(),
                quote.isBerryc(),
                quote.getPcbaPlant(),
                quote.getPcbOrigin(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getQuantities().stream().map(QuoteService::toQuantityResponse).toList(),
                historyRepository.findAllByQidOrderByChangedAtDesc(quote.getQid()).stream()
                        .map(QuoteService::toHistoryResponse).toList(),
                lockService.find(quote.getQid(), userId)
        );
    }

    private static QuoteQuantityResponse toQuantityResponse(QuoteQuantity quantity) {
        return new QuoteQuantityResponse(
                quantity.getQtyId(),
                quantity.getQuote().getQid(),
                quantity.getLt(),
                quantity.getLtPcb(),
                quantity.getLtPcba(),
                quantity.getQty(),
                quantity.getPcbCost(),
                quantity.getPartsCost(),
                quantity.getLaborCost(),
                quantity.getLaborDiscount(),
                quantity.getPartMarkup(),
                quantity.getTesting(),
                quantity.getConfCoat(),
                quantity.getServiceName(),
                quantity.getServiceCharge(),
                quantity.getSubTotal(),
                quantity.getTotalSalesPct(),
                quantity.getPcbNre(),
                quantity.getAssyNre(),
                quantity.getStencil(),
                quantity.getOtherNre(),
                quantity.getTotal(),
                quantity.getComments(),
                quantity.isReceived()
        );
    }

    private static QuoteHistoryResponse toHistoryResponse(QuoteHistory entry) {
        return new QuoteHistoryResponse(
                entry.getQhId(),
                entry.getQid(),
                entry.getAction(),
                entry.getStatus(),
                entry.getAssyQuoteStatus(),
                entry.getPcbQuoteStatus(),
                entry.getChangedByUserId(),
                entry.getChangedByName(),
                entry.getChangedAt()
        );
    }

    private static String displayName(AppUser user) {
        return user == null ? "Unknown" : user.getDisplayName();
    }

    private static boolean isReceived(String status) {
        return status != null && status.trim().equalsIgnoreCase(RECEIVED_STATUS);
    }

    /** Roles allowed to change create_date. Add the rest here once they are agreed. */
    private static boolean mayEditCreateDate(AppUser user) {
        return user != null && user.getRoles().stream()
                .anyMatch(role -> CREATE_DATE_ROLES.contains(role.getRoleCode()));
    }

    /**
     * There is no Bean Validation provider on the classpath, so the request
     * constraints are checked here rather than trusting the annotations.
     */
    private static String requireQuoteNumber(QuoteRequest request) {
        if (request.quoteNumber() == null || request.quoteNumber().isBlank()) {
            throw new QuoteValidationException("quoteNumber is required");
        }
        if (request.createDate() == null) {
            throw new QuoteValidationException("createDate is required");
        }
        return request.quoteNumber().trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isTrue(Boolean value) {
        return value != null && value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
