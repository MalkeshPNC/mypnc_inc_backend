package com.pnc.masters.quote.application;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.ncmaster.NcMaster;
import com.pnc.masters.ncmaster.NcMasterRepository;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteHistory;
import com.pnc.masters.quote.QuoteHistoryRepository;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteCopyNumberResponse;
import com.pnc.masters.quote.api.QuoteFamilyResponse;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class QuoteService {

    private static final Set<String> CREATE_DATE_ROLES = Set.of("ADMIN");

    /** The quotestatus entry that means the quote went out to the customer. */
    private static final String SUBMITTED_STATUS = "submitted";

    /** The quotestatus entry that means the quote came back from the customer. */
    private static final String RECEIVED_STATUS = "received";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** History wording for the two halves of the edit screen. */
    private static final String MAIN_CHANGE = "Changes in Main table";
    private static final String QUANTITY_CHANGE = "Changes in Quantity table";

    private final QuoteRepository quoteRepository;
    private final QuoteHistoryRepository historyRepository;
    private final QuoteLockService lockService;
    private final AppUserRepository userRepository;
    private final NcMasterRepository ncMasterRepository;
    private final ContactRepository contactRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        QuoteHistoryRepository historyRepository,
                        QuoteLockService lockService,
                        AppUserRepository userRepository,
                        NcMasterRepository ncMasterRepository,
                        ContactRepository contactRepository) {
        this.quoteRepository = quoteRepository;
        this.historyRepository = historyRepository;
        this.lockService = lockService;
        this.userRepository = userRepository;
        this.ncMasterRepository = ncMasterRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional(readOnly = true)
    public List<QuoteSummaryResponse> findAll(Long userId) {
        List<Quote> quotes = quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();
        Map<Long, QuoteLockResponse> locks =
                lockService.findAll(quotes.stream().map(Quote::getQid).toList(), userId);
        Map<String, Integer> familyCounts = familyCounts(quotes);
        return quotes.stream()
                .map(quote -> toSummary(quote, locks.get(quote.getQid()), familySize(quote, familyCounts)))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(Long id, Long userId) {
        return toResponse(getQuote(id), userId, true);
    }

    /**
     * All non-deleted quotes in the copy family, original first then numeric suffix.
     * History and lock are omitted; compare does not need them.
     */
    @Transactional(readOnly = true)
    public QuoteFamilyResponse findFamily(Long id, Long userId) {
        Quote source = getQuote(id);
        String family = quoteFamily(source.getQuoteNumber());
        List<Quote> members = new ArrayList<>(quoteRepository.findFamilyQuotes(family));
        members.sort(Comparator.comparingInt(quote -> familySortKey(quote.getQuoteNumber(), family)));
        int size = members.size();
        List<QuoteResponse> quotes = members.stream()
                .map(quote -> toResponse(quote, userId, false, size))
                .toList();
        return new QuoteFamilyResponse(family, quotes);
    }

    /**
     * Next Quote# in the copy family. The unsuffixed original is version 1, so
     * the first copy is {@code family.2}, then {@code family.(maxSuffix + 1)}.
     * Gaps are not filled.
     */
    @Transactional(readOnly = true)
    public QuoteCopyNumberResponse nextCopyNumber(Long id) {
        Quote quote = getQuote(id);
        String family = quoteFamily(quote.getQuoteNumber());
        List<String> numbers = quoteRepository.findFamilyQuoteNumbers(family);
        Pattern suffix = Pattern.compile("(?i)^" + Pattern.quote(family) + "\\.(\\d+)$");
        // The bare family number is revision 1; copies start at .2.
        int maxSuffix = 1;
        for (String number : numbers) {
            if (number == null) {
                continue;
            }
            Matcher matcher = suffix.matcher(number.trim());
            if (matcher.matches()) {
                maxSuffix = Math.max(maxSuffix, Integer.parseInt(matcher.group(1)));
            }
        }
        return new QuoteCopyNumberResponse(family + "." + (maxSuffix + 1));
    }

    /**
     * Creates the header, then persists quantity rows when the request includes
     * them (copy). qtyId 0 / null is treated as a new line.
     */
    public QuoteResponse create(QuoteRequest request, Long userId) {
        String quoteNumber = requireQuoteNumber(request);
        if (quoteRepository.existsByQuoteNumberIgnoreCase(quoteNumber)) {
            throw new QuoteNumberExistsException(quoteNumber);
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        Quote quote = new Quote();
        applyRequest(quote, request, quoteNumber);
        seedBlankFieldsFromNc(quote);
        stampQuotePersonsIfStatusChanged(quote, request, user, null, null);
        quote.setCreatedByUserId(userId);
        Quote saved = quoteRepository.save(quote);
        List<QuoteQuantityRequest> rows = request.quantities();
        if (rows != null && !rows.isEmpty()) {
            applyQuantities(saved, rows);
            saved = quoteRepository.save(saved);
        }
        recordHistory(saved, QuoteHistory.ACTION_CREATED, userId, user, null);
        return toResponse(saved, userId, true);
    }

    public QuoteResponse update(Long id, QuoteRequest request, Long userId) {
        Quote quote = getQuote(id);
        lockService.requireHeldBy(id, userId);
        String quoteNumber = requireQuoteNumber(request);
        if (quoteRepository.existsByQuoteNumberIgnoreCaseAndQidNot(quoteNumber, id)) {
            throw new QuoteNumberExistsException(quoteNumber);
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        String previousAqStatus = quote.getAssyQuoteStatus();
        String previousPqStatus = quote.getPcbQuoteStatus();
        LocalDate createDate = quote.getCreateDate();
        List<Object> headerBefore = headerSignature(quote);
        List<List<Object>> quantitiesBefore = quantitySignature(quote);
        applyRequest(quote, request, quoteNumber);
        stampQuotePersonsIfStatusChanged(quote, request, user, previousAqStatus, previousPqStatus);
        // Create date is role-gated in the UI, so hold the line here rather than
        // trusting whatever the client posted.
        if (!mayEditCreateDate(user)) {
            quote.setCreateDate(createDate);
        }
        applyQuantities(quote, request.quantities());
        quote.setUpdatedByUserId(userId);
        quote.setUpdatedAt(LocalDateTime.now());
        Quote saved = quoteRepository.save(quote);
        recordHistory(saved, QuoteHistory.ACTION_UPDATED, userId, user,
                describeChanges(headerBefore, quantitiesBefore, saved));
        return toResponse(saved, userId, true);
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

    /**
     * Family is the quote number with a trailing {@code .digits} stripped, so
     * {@code 2026NC-100} and {@code 2026NC-100.1} share {@code 2026NC-100}.
     * Copying {@code .1} still uses that family, not {@code .1.1}.
     */
    static String quoteFamily(String quoteNumber) {
        if (quoteNumber == null || quoteNumber.isBlank()) {
            return "";
        }
        return quoteNumber.trim().replaceFirst("\\.\\d+$", "");
    }

    private static Map<String, Integer> familyCounts(List<Quote> quotes) {
        Map<String, Integer> counts = new HashMap<>();
        for (Quote quote : quotes) {
            String family = quoteFamily(quote.getQuoteNumber());
            if (!family.isBlank()) {
                counts.merge(family.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static int familySize(Quote quote, Map<String, Integer> familyCounts) {
        String family = quoteFamily(quote.getQuoteNumber());
        if (family.isBlank()) {
            return 1;
        }
        return familyCounts.getOrDefault(family.toLowerCase(Locale.ROOT), 1);
    }

    private int familySize(Quote quote) {
        String family = quoteFamily(quote.getQuoteNumber());
        if (family.isBlank()) {
            return 1;
        }
        return Math.max(1, quoteRepository.findFamilyQuoteNumbers(family).size());
    }

    /**
     * Original (unsuffixed) first, then {@code family.2}, {@code family.3}, …
     */
    static int familySortKey(String quoteNumber, String family) {
        if (quoteNumber == null || quoteNumber.isBlank()) {
            return Integer.MAX_VALUE;
        }
        String trimmed = quoteNumber.trim();
        if (family != null && trimmed.equalsIgnoreCase(family)) {
            return 0;
        }
        if (family == null || family.isBlank()) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = Pattern.compile("(?i)^" + Pattern.quote(family) + "\\.(\\d+)$").matcher(trimmed);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return Integer.MAX_VALUE;
    }

    private void applyRequest(Quote quote, QuoteRequest request, String quoteNumber) {
        quote.setQuoteNumber(quoteNumber);
        quote.setQuoteType(blankToNull(request.quoteType()));
        quote.setProjectNumber(blankToNull(request.projectNumber()));
        quote.setCreateDate(request.createDate());
        quote.setSubmitDate(request.submitDate());
        // A submitted quote always has a submit date, whatever the client sent.
        if (quote.getSubmitDate() == null && isStatus(request.status(), SUBMITTED_STATUS)) {
            quote.setSubmitDate(LocalDate.now());
        }
        quote.setCustId(request.custId());
        quote.setCustomerName(blankToNull(request.customerName()));
        quote.setContId(request.contId());
        quote.setContactName(resolveContactName(request.contId(), request.contactName()));
        quote.setCustomerRfq(blankToNull(request.customerRfq()));
        quote.setNcId(request.ncId());
        quote.setNcNumber(blankToNull(request.ncNumber()));
        quote.setAssyNumber(blankToNull(request.assyNumber()));
        quote.setPcbaRevision(blankToNull(request.pcbaRevision()));
        quote.setPcbNumber(blankToNull(request.pcbNumber()));
        quote.setPcbRevision(blankToNull(request.pcbRevision()));
        quote.setAssyQuoteStatus(blankToNull(request.assyQuoteStatus()));
        quote.setPcbQuoteNumber(blankToNull(request.pcbQuoteNumber()));
        quote.setPcbQuoteStatus(blankToNull(request.pcbQuoteStatus()));
        quote.setQuoteArray(blankToNull(request.array()));
        quote.setCommissionPercentage(request.commissionPercentage());
        quote.setInternalNote1(blankToNull(request.internalNote1()));
        quote.setInternalNote2(blankToNull(request.internalNote2()));
        quote.setNotesToCustomer(blankToNull(request.notesToCustomer()));
        quote.setOtherNreCharges(blankToNull(request.otherNreCharges()));
        quote.setReceivedDate(request.receivedDate());
        // A received quote always has a received date, whatever the client sent.
        if (quote.getReceivedDate() == null && isStatus(request.status(), RECEIVED_STATUS)) {
            quote.setReceivedDate(LocalDate.now());
        }
        quote.setPncNotes(isTrue(request.pncNotes()));
        quote.setLaborOnly(isTrue(request.laborOnly()));
        quote.setPartsScheduled(isTrue(request.partsScheduled()));
        quote.setFeedback(isTrue(request.feedback()));
        quote.setItarc(isTrue(request.itarc()));
        quote.setBerryc(isTrue(request.berryc()));
        quote.setSamsReview(isTrue(request.samsReview()));
        quote.setPcbaPlant(blankToNull(request.pcbaPlant()));
        quote.setPcbOrigin(blankToNull(request.pcbOrigin()));
        quote.setStatus(blankToNull(request.status()));
    }

    /**
     * Each person column is stamped only when its matching status actually
     * moved. Empty and null count as the same status so a first save of a
     * blank field does not overwrite a stored name.
     */
    private void stampQuotePersonsIfStatusChanged(
            Quote quote, QuoteRequest request, AppUser user, String previousAq, String previousPq) {
        String personName = displayName(user);
        if (!sameQuoteStatus(previousAq, request.assyQuoteStatus())) {
            quote.setAssyQuotePerson(personName);
        }
        if (!sameQuoteStatus(previousPq, request.pcbQuoteStatus())) {
            quote.setPcbQuotePerson(personName);
        }
    }

    private static boolean sameQuoteStatus(String stored, String requested) {
        String left = stored == null ? "" : stored.trim();
        String right = requested == null ? "" : requested.trim();
        return left.equalsIgnoreCase(right);
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

        BigDecimal commission = quote.getCommissionPercentage();
        for (QuoteQuantityRequest row : rows) {
            boolean isNew = row.qtyId() == null || row.qtyId() <= 0;
            QuoteQuantity quantity = isNew ? null : existing.get(row.qtyId());
            if (quantity == null) {
                quantity = new QuoteQuantity();
                applyQuantity(quantity, row, commission);
                quote.addQuantity(quantity);
            } else {
                applyQuantity(quantity, row, commission);
            }
        }
    }

    private void applyQuantity(QuoteQuantity quantity, QuoteQuantityRequest row, BigDecimal commission) {
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
        quantity.setServiceCharge(row.serviceCharge());
        quantity.setPcbNre(row.pcbNre());
        quantity.setAssyNre(row.assyNre());
        quantity.setStencil(row.stencil());
        quantity.setOtherNre(row.otherNre());
        quantity.setComments(blankToNull(row.comments()));
        quantity.setReceived(isTrue(row.received()));
        // subTotal, totalSalesPct and total are derived, so whatever the client
        // posted for them is discarded in favour of the numbers below.
        recalculate(quantity, commission);
    }

    /**
     * Fills in the three derived amounts. Each one is rounded before it feeds the
     * next, so the figures on screen add up when someone checks them by hand.
     */
    private static void recalculate(QuoteQuantity quantity, BigDecimal commission) {
        BigDecimal partsNet = plusPercent(quantity.getPartsCost(), quantity.getPartMarkup());
        BigDecimal laborNet = plusPercent(quantity.getLaborCost(), negate(quantity.getLaborDiscount()));
        BigDecimal subTotal = money(zeroIfNull(quantity.getPcbCost())
                .add(partsNet)
                .add(laborNet)
                .add(zeroIfNull(quantity.getTesting()))
                .add(zeroIfNull(quantity.getConfCoat()))
                .add(zeroIfNull(quantity.getServiceCharge())));
        BigDecimal totalSalesPct = money(plusPercent(subTotal, commission));
        BigDecimal total = money(totalSalesPct
                .multiply(BigDecimal.valueOf(quantity.getQty() == null ? 0 : quantity.getQty()))
                .add(zeroIfNull(quantity.getPcbNre()))
                .add(zeroIfNull(quantity.getAssyNre()))
                .add(zeroIfNull(quantity.getStencil()))
                .add(zeroIfNull(quantity.getOtherNre())));

        quantity.setSubTotal(subTotal);
        quantity.setTotalSalesPct(totalSalesPct);
        quantity.setTotal(total);
    }

    /** {@code amount + amount * percent / 100}, unrounded so callers decide the scale. */
    private static BigDecimal plusPercent(BigDecimal amount, BigDecimal percent) {
        BigDecimal base = zeroIfNull(amount);
        if (percent == null || percent.signum() == 0) {
            return base;
        }
        // Scale 4 on the factor keeps a two-decimal percentage exact.
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(HUNDRED, 4, RoundingMode.HALF_UP));
        return base.multiply(factor);
    }

    private static BigDecimal negate(BigDecimal value) {
        return value == null ? null : value.negate();
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Names the parts of the quote this save touched, so the history reads as
     * more than a list of timestamps. Null when the save changed nothing.
     */
    private static String describeChanges(List<Object> headerBefore, List<List<Object>> quantitiesBefore, Quote saved) {
        List<String> parts = new ArrayList<>();
        if (!headerBefore.equals(headerSignature(saved))) {
            parts.add(MAIN_CHANGE);
        }
        if (!quantitiesBefore.equals(quantitySignature(saved))) {
            parts.add(QUANTITY_CHANGE);
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    /** Every header field {@link #applyRequest} can write, in a comparable form. */
    private static List<Object> headerSignature(Quote quote) {
        return Arrays.asList(
                quote.getQuoteNumber(), quote.getQuoteType(), quote.getProjectNumber(),
                quote.getCreateDate(), quote.getSubmitDate(), quote.getReceivedDate(),
                quote.getCustId(), quote.getCustomerName(), quote.getContId(), quote.getContactName(),
                quote.getCustomerRfq(), quote.getNcId(), quote.getNcNumber(),
                quote.getAssyNumber(), quote.getPcbaRevision(), quote.getPcbNumber(), quote.getPcbRevision(),
                quote.getAssyQuoteStatus(), quote.getPcbQuoteNumber(), quote.getPcbQuoteStatus(),
                quote.getQuoteArray(), norm(quote.getCommissionPercentage()),
                quote.getInternalNote1(), quote.getInternalNote2(), quote.getNotesToCustomer(),
                quote.getOtherNreCharges(), quote.isPncNotes(), quote.isLaborOnly(),
                quote.isPartsScheduled(), quote.isFeedback(), quote.isItarc(), quote.isBerryc(),
                quote.isSamsReview(), quote.getPcbaPlant(), quote.getPcbOrigin(), quote.getStatus()
        );
    }

    /** One entry per line, so an added, removed or edited row all show up. */
    private static List<List<Object>> quantitySignature(Quote quote) {
        return quote.getQuantities().stream()
                .map(row -> Arrays.asList(
                        row.getQtyId(), row.getLt(), row.getLtPcb(), row.getLtPcba(), row.getQty(),
                        norm(row.getPcbCost()), norm(row.getPartsCost()), norm(row.getLaborCost()),
                        norm(row.getLaborDiscount()), norm(row.getPartMarkup()), norm(row.getTesting()),
                        norm(row.getConfCoat()), row.getServiceName(), norm(row.getServiceCharge()),
                        norm(row.getSubTotal()), norm(row.getTotalSalesPct()), norm(row.getPcbNre()),
                        norm(row.getAssyNre()), norm(row.getStencil()), norm(row.getOtherNre()),
                        norm(row.getTotal()), row.getComments(), row.isReceived()
                ))
                .toList();
    }

    /** 5 and 5.00 are the same amount, so compare without the trailing zeros. */
    private static Object norm(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private void recordHistory(Quote quote, String action, Long userId, AppUser user, String changeSummary) {
        QuoteHistory entry = new QuoteHistory();
        entry.setQid(quote.getQid());
        entry.setAction(action);
        entry.setChangeSummary(changeSummary);
        entry.setStatus(quote.getStatus());
        entry.setAssyQuoteStatus(quote.getAssyQuoteStatus());
        entry.setPcbQuoteStatus(quote.getPcbQuoteStatus());
        entry.setChangedByUserId(userId);
        entry.setChangedByName(displayName(user));
        entry.setChangedAt(LocalDateTime.now());
        historyRepository.save(entry);
    }

    private QuoteSummaryResponse toSummary(Quote quote, QuoteLockResponse lock, int familySize) {
        return new QuoteSummaryResponse(
                quote.getQid(),
                quote.getQuoteNumber(),
                quote.getQuoteType(),
                quote.getProjectNumber(),
                quote.getCreateDate(),
                quote.getSubmitDate(),
                quote.getReceivedDate(),
                quote.getCustomerName(),
                quote.getNcNumber(),
                quote.getAssyNumber(),
                quote.getPcbNumber(),
                quote.getAssyQuoteStatus(),
                quote.getPcbQuoteStatus(),
                quote.getStatus(),
                lock,
                familySize
        );
    }

    private QuoteResponse toResponse(Quote quote, Long userId, boolean includeAudit) {
        return toResponse(quote, userId, includeAudit, familySize(quote));
    }

    private QuoteResponse toResponse(Quote quote, Long userId, boolean includeAudit, int familySize) {
        NcDerived displayed = displayNcDerived(quote);
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
                displayedContactName(quote),
                quote.getCustomerRfq(),
                quote.getNcId(),
                quote.getNcNumber(),
                displayed.assyNumber(),
                displayed.pcbaRevision(),
                displayed.pcbNumber(),
                displayed.pcbRevision(),
                quote.getAssyQuoteStatus(),
                quote.getAssyQuotePerson(),
                quote.getPcbQuoteNumber(),
                quote.getPcbQuoteStatus(),
                quote.getPcbQuotePerson(),
                quote.getQuoteArray(),
                quote.getCommissionPercentage(),
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
                quote.isSamsReview(),
                quote.getPcbaPlant(),
                quote.getPcbOrigin(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getQuantities().stream().map(QuoteService::toQuantityResponse).toList(),
                includeAudit
                        ? historyRepository.findAllByQidOrderByChangedAtDesc(quote.getQid()).stream()
                                .map(QuoteService::toHistoryResponse).toList()
                        : List.of(),
                includeAudit ? lockService.find(quote.getQid(), userId) : null,
                familySize
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
                entry.getChangeSummary(),
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

    private static boolean isStatus(String status, String target) {
        return status != null && status.trim().equalsIgnoreCase(target);
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

    private String displayedContactName(Quote quote) {
        return resolveContactName(quote.getContId(), quote.getContactName());
    }

    private String resolveContactName(Long contId, String storedName) {
        String name = blankToNull(storedName);
        if (name != null || contId == null) {
            return name;
        }
        return contactRepository.findById(contId).map(QuoteService::contactLabel).orElse(null);
    }

    private static String contactLabel(Contact contact) {
        String name = joinName(contact.getFirstName(), contact.getLastName());
        if (name != null) {
            return name;
        }
        String person = blankToNull(contact.getContactPerson());
        if (person != null) {
            return person;
        }
        String email = blankToNull(contact.getEmail());
        if (email != null) {
            return email;
        }
        String phone = blankToNull(contact.getPhone());
        return phone != null ? phone : "Contact #" + contact.getContId();
    }

    private static String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String joined = (first + " " + last).trim();
        return joined.isEmpty() ? null : joined;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * First save copies any still-empty NC fields onto the quote so the edit
     * screen has something to show before the user has updated it.
     */
    private void seedBlankFieldsFromNc(Quote quote) {
        findNc(quote.getNcId()).ifPresent(nc -> {
            if (isBlank(quote.getAssyNumber())) {
                quote.setAssyNumber(blankToNull(nc.getPcbaPartNumber()));
            }
            if (isBlank(quote.getPcbNumber())) {
                quote.setPcbNumber(blankToNull(nc.getPcbPartNumber()));
            }
            if (isBlank(quote.getPcbaRevision())) {
                quote.setPcbaRevision(blankToNull(nc.getPcbaRev()));
            }
            if (isBlank(quote.getPcbRevision())) {
                quote.setPcbRevision(blankToNull(nc.getPcbRev()));
            }
        });
    }

    /**
     * Until the quote has been updated, PCB/PCBA rev always come from the NC.
     * Assy# and PCB# do the same only when they no longer match the NC.
     */
    private NcDerived displayNcDerived(Quote quote) {
        if (quote.getUpdatedAt() != null) {
            return new NcDerived(
                    quote.getAssyNumber(),
                    quote.getPcbaRevision(),
                    quote.getPcbNumber(),
                    quote.getPcbRevision()
            );
        }
        return findNc(quote.getNcId())
                .map(nc -> new NcDerived(
                        firstVisitPart(quote.getAssyNumber(), nc.getPcbaPartNumber()),
                        preferNc(nc.getPcbaRev(), quote.getPcbaRevision()),
                        firstVisitPart(quote.getPcbNumber(), nc.getPcbPartNumber()),
                        preferNc(nc.getPcbRev(), quote.getPcbRevision())
                ))
                .orElseGet(() -> new NcDerived(
                        quote.getAssyNumber(),
                        quote.getPcbaRevision(),
                        quote.getPcbNumber(),
                        quote.getPcbRevision()
                ));
    }

    private Optional<NcMaster> findNc(Long ncId) {
        return ncId == null ? Optional.empty() : ncMasterRepository.findById(ncId);
    }

    private static String firstVisitPart(String stored, String ncValue) {
        if (isBlank(stored) || sameQuoteStatus(stored, ncValue)) {
            return blankToNull(stored) != null ? blankToNull(stored) : blankToNull(ncValue);
        }
        return blankToNull(ncValue);
    }

    private static String preferNc(String ncValue, String stored) {
        return blankToNull(ncValue) != null ? blankToNull(ncValue) : blankToNull(stored);
    }

    private record NcDerived(String assyNumber, String pcbaRevision, String pcbNumber, String pcbRevision) {
    }
}
