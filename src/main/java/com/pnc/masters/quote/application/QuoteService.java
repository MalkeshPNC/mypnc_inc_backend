package com.pnc.masters.quote.application;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.ncmaster.NcMaster;
import com.pnc.masters.ncmaster.NcMasterRepository;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteAttachmentRepository;
import com.pnc.masters.quote.QuoteContact;
import com.pnc.masters.quote.QuoteHistory;
import com.pnc.masters.quote.QuoteHistoryRepository;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteContactRequest;
import com.pnc.masters.quote.api.QuoteContactResponse;
import com.pnc.masters.quote.api.QuoteCopyNumberResponse;
import com.pnc.masters.quote.api.QuoteFamilyResponse;
import com.pnc.masters.quote.api.QuoteHistoryResponse;
import com.pnc.masters.quote.api.QuoteLockResponse;
import com.pnc.masters.quote.api.QuoteNotFoundException;
import com.pnc.masters.quote.api.QuoteNcChangeRequest;
import com.pnc.masters.quote.api.QuoteNumberExistsException;
import com.pnc.masters.quote.api.QuotePageResponse;
import com.pnc.masters.quote.api.QuoteQuantityRequest;
import com.pnc.masters.quote.api.QuoteQuantityResponse;
import com.pnc.masters.quote.api.QuoteRequest;
import com.pnc.masters.quote.api.QuoteResponse;
import com.pnc.masters.quote.api.QuoteSummaryResponse;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    /** Stands in for "no quote to leave out" in the number lookups. */
    private static final Long NO_QUOTE = 0L;

    private static final int PAGE_SIZE = 100;

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
    private final QuoteAttachmentRepository attachmentRepository;
    private final Clock clock;

    public QuoteService(QuoteRepository quoteRepository,
                        QuoteHistoryRepository historyRepository,
                        QuoteLockService lockService,
                        AppUserRepository userRepository,
                        NcMasterRepository ncMasterRepository,
                        ContactRepository contactRepository,
                        QuoteAttachmentRepository attachmentRepository,
                        Clock clock) {
        this.quoteRepository = quoteRepository;
        this.historyRepository = historyRepository;
        this.lockService = lockService;
        this.userRepository = userRepository;
        this.ncMasterRepository = ncMasterRepository;
        this.contactRepository = contactRepository;
        this.attachmentRepository = attachmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<QuoteSummaryResponse> findAll(Long userId) {
        List<Quote> quotes = quoteRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();
        List<Long> qids = quotes.stream().map(Quote::getQid).toList();
        Map<Long, QuoteLockResponse> locks = lockService.findAll(qids, userId);
        Map<String, Integer> ncCounts = ncCounts(quotes);
        Map<Long, BigDecimal> receivedTotals = receivedTotals(qids);
        return quotes.stream()
                .map(quote -> toSummary(
                        quote,
                        locks.get(quote.getQid()),
                        ncSize(quote, ncCounts),
                        receivedTotals.getOrDefault(quote.getQid(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuotePageResponse page(Long userId, int page, Integer size, String status, String field, String query) {
        int pageSize = size == null || size < 1 || size > PAGE_SIZE ? PAGE_SIZE : size;
        int pageIndex = Math.max(page, 0);
        Page<Quote> result = quoteRepository.findAll(
                pageSpec(status, field, query),
                PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("qid")))
        );
        List<Quote> quotes = result.getContent();
        List<Long> qids = quotes.stream().map(Quote::getQid).toList();
        Map<Long, QuoteLockResponse> locks = lockService.findAll(qids, userId);
        Map<String, Integer> familySizes = ncCountsForPage(quotes);
        Map<Long, BigDecimal> received = receivedTotals(qids);
        List<QuoteSummaryResponse> items = quotes.stream()
                .map(quote -> toSummary(
                        quote,
                        locks.get(quote.getQid()),
                        ncSize(quote, familySizes),
                        received.getOrDefault(quote.getQid(), BigDecimal.ZERO)))
                .toList();
        return new QuotePageResponse(items, result.getTotalElements(), pageIndex, pageSize);
    }

    private Map<String, Integer> ncCountsForPage(List<Quote> quotes) {
        List<String> ncNumbers = quotes.stream()
                .map(quote -> ncText(quote.getNcNumber()).toLowerCase(Locale.ROOT))
                .filter(nc -> !nc.isEmpty())
                .distinct()
                .toList();
        if (ncNumbers.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (Object[] row : quoteRepository.countByNcNumber(ncNumbers)) {
            if (row[0] instanceof String nc && row[1] instanceof Number count) {
                counts.put(nc, count.intValue());
            }
        }
        return counts;
    }

    private static Specification<Quote> pageSpec(String status, String field, String query) {
        String statusKey = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return (root, criteria, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (!statusKey.isEmpty() && !"all".equals(statusKey)) {
                predicates.add(cb.equal(cb.lower(cb.trim(root.get("status"))), statusKey));
            }
            Expression<String> column = searchColumn(root, field);
            if (!text.isEmpty() && column != null) {
                predicates.add(cb.like(cb.lower(column), likePattern(text), '\\'));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Expression<String> searchColumn(jakarta.persistence.criteria.Root<Quote> root, String field) {
        if (field == null) {
            return null;
        }
        return switch (field) {
            case "qid" -> root.get("qid").as(String.class);
            case "quoteNumber" -> root.get("quoteNumber");
            case "projectNumber" -> root.get("projectNumber");
            case "customerName" -> root.get("customerName");
            case "assyNumber" -> root.get("assyNumber");
            case "pcbNumber" -> root.get("pcbNumber");
            case "assyQuotePerson" -> root.get("assyQuotePerson");
            case "pcbQuotePerson" -> root.get("pcbQuotePerson");
            default -> null;
        };
    }

    private static String likePattern(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(Long id, Long userId) {
        return toResponse(getQuote(id), userId, true);
    }

    /**
     * Every non-deleted quote with the same NC, oldest first.
     * A blank NC compares as itself. History and lock are omitted.
     */
    @Transactional(readOnly = true)
    public QuoteFamilyResponse findFamily(Long id, Long userId) {
        Quote source = getQuote(id);
        String nc = ncText(source.getNcNumber());
        List<Quote> members = nc.isEmpty()
                ? new ArrayList<>(List.of(source))
                : new ArrayList<>(quoteRepository.findByNcNumberIgnoreCaseAndIsDeletedFalse(nc));
        members.sort(Comparator
                .comparing(Quote::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Quote::getQid, Comparator.nullsLast(Comparator.naturalOrder())));
        int size = members.size();
        List<QuoteResponse> quotes = members.stream()
                .map(quote -> toResponse(quote, userId, false, size))
                .toList();
        return new QuoteFamilyResponse(nc, quotes);
    }

    /**
     * Next Quote# for a copy. Same rule as a new quote: current year plus the
     * NC, and one more than the highest revision of that NC in any year.
     */
    @Transactional(readOnly = true)
    public QuoteCopyNumberResponse nextCopyNumber(Long id) {
        Quote quote = getQuote(id);
        String nc = ncOf(quote);
        if (nc.isEmpty()) {
            throw new QuoteValidationException("ncNumber is required");
        }
        return numberForNc(nc, NO_QUOTE);
    }

    /**
     * Quote# for an unsaved quote. A saved quote keeps the number it already has.
     * When the NC is new the number is the current year plus the NC. When any
     * quote already uses that NC, the number always carries the next revision.
     */
    @Transactional(readOnly = true)
    public QuoteCopyNumberResponse nextNumberForNc(Long qid, String ncNumber) {
        if (ncNumber == null || ncNumber.isBlank()) {
            throw new QuoteValidationException("ncNumber is required");
        }
        if (qid != null) {
            return new QuoteCopyNumberResponse(getQuote(qid).getQuoteNumber());
        }
        return numberForNc(ncNumber, NO_QUOTE);
    }

    /**
     * Current year plus the NC. No saved quote for that NC stays unsuffixed.
     * Otherwise the suffix is one more than the highest {@code .n} in any year.
     * A number with no suffix counts as revision 0. Gaps are not filled.
     * {@code excludeQid} is left out of the count so a quote being renumbered
     * does not collide with itself.
     */
    private QuoteCopyNumberResponse numberForNc(String ncNumber, Long excludeQid) {
        String nc = ncNumber.trim().toUpperCase(Locale.ROOT);
        String base = Year.now(clock).getValue() + nc;
        List<String> numbers = quoteRepository.findQuoteNumbersForNc(nc, likeLiteral(nc), excludeQid);
        if (numbers.isEmpty()) {
            return new QuoteCopyNumberResponse(base);
        }
        return new QuoteCopyNumberResponse(base + "." + (highestRevision(numbers, nc) + 1));
    }

    /** Highest {@code .n} for this NC; 0 when every match is unsuffixed or unparsed. */
    private static int highestRevision(List<String> numbers, String nc) {
        Pattern suffix = Pattern.compile("(?i)^\\d{4}" + Pattern.quote(nc) + "(?:\\.(\\d+))?$");
        int highest = 0;
        for (String number : numbers) {
            if (number == null) {
                continue;
            }
            Matcher matcher = suffix.matcher(number.trim());
            if (matcher.matches() && matcher.group(1) != null) {
                highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
            }
        }
        return highest;
    }

    /** LIKE pattern for an NC, so {@code %} and {@code _} in the NC stay literal. */
    private static String likeLiteral(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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
        requirePNotes(id, request);
        requireUnchangedNc(quote, request);
        String quoteNumber = quote.getQuoteNumber();
        AppUser user = userRepository.findById(userId).orElse(null);
        LocalDate createDate = quote.getCreateDate();
        List<Object> headerBefore = headerSignature(quote);
        List<List<Object>> quantitiesBefore = quantitySignature(quote);
        applyRequest(quote, request, quoteNumber);
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

    /**
     * Points a saved quote at another NC master and copies that NC's part numbers
     * onto the quote. Quote# is the next number for the new NC. Quantities, status,
     * and dates other than the update stamp stay as they are. A normal update
     * still rejects an NC change.
     */
    public QuoteResponse changeNc(Long id, QuoteNcChangeRequest request, Long userId) {
        if (request == null || request.ncId() == null) {
            throw new QuoteValidationException("ncId is required");
        }
        Quote quote = getQuote(id);
        lockService.requireHeldBy(id, userId);
        NcMaster nc = ncMasterRepository.findById(request.ncId())
                .orElseThrow(() -> new QuoteValidationException("NC was not found"));
        String nextNumber = ncText(nc.getNcNumber());
        if (nextNumber.isEmpty()) {
            throw new QuoteValidationException("ncNumber is required");
        }
        String previous = ncText(quote.getNcNumber());
        if (sameId(quote.getNcId(), nc.getNcId()) && sameNc(previous, nextNumber)) {
            return toResponse(quote, userId, true);
        }
        String previousQuoteNumber = ncText(quote.getQuoteNumber());
        String quoteNumber = numberForNc(nextNumber, quote.getQid()).quoteNumber();
        if (quoteRepository.existsByQuoteNumberIgnoreCaseAndQidNot(quoteNumber, quote.getQid())) {
            throw new QuoteNumberExistsException(quoteNumber);
        }
        quote.setNcId(nc.getNcId());
        quote.setNcNumber(nextNumber);
        quote.setQuoteNumber(quoteNumber);
        quote.setAssyNumber(blankToNull(nc.getPcbaPartNumber()));
        quote.setPcbaRevision(blankToNull(nc.getPcbaRev()));
        quote.setPcbNumber(blankToNull(nc.getPcbPartNumber()));
        quote.setPcbRevision(blankToNull(nc.getPcbRev()));
        quote.setUpdatedByUserId(userId);
        quote.setUpdatedAt(LocalDateTime.now());
        Quote saved = quoteRepository.save(quote);
        AppUser user = userRepository.findById(userId).orElse(null);
        String from = previous.isEmpty() ? "—" : previous;
        String summary = "NC# " + from + " → " + nextNumber;
        if (!previousQuoteNumber.equalsIgnoreCase(quoteNumber)) {
            String oldNumber = previousQuoteNumber.isEmpty() ? "—" : previousQuoteNumber;
            summary += ". Quote# " + oldNumber + " → " + quoteNumber;
        }
        recordHistory(saved, QuoteHistory.ACTION_UPDATED, userId, user, summary);
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
     * NC used to number a copy. A blank stored NC is read out of the quote
     * number, after the year and without a trailing revision.
     */
    private static String ncOf(Quote quote) {
        String stored = ncText(quote.getNcNumber());
        if (!stored.isEmpty()) {
            return stored.toUpperCase(Locale.ROOT);
        }
        String number = quote.getQuoteNumber();
        if (number == null || number.isBlank()) {
            return "";
        }
        String body = number.trim().replaceFirst("(?i)\\.\\d+$", "");
        return body.replaceFirst("^\\d{4}", "").toUpperCase(Locale.ROOT);
    }

    private static String ncText(String ncNumber) {
        return ncNumber == null ? "" : ncNumber.trim();
    }

    private void requirePNotes(Long qid, QuoteRequest request) {
        if (!isTrue(request.pncNotes()) && !isTrue(request.laborOnly())) {
            return;
        }
        if (!attachmentRepository.existsByQidAndCategory(qid, QuoteAttachmentService.P_NOTES)) {
            throw new QuoteValidationException(
                    "Upload a p_notes document before turning on PNC Notes or Labor Only.");
        }
    }

    private static void requireUnchangedNc(Quote quote, QuoteRequest request) {
        if (!sameNc(quote.getNcNumber(), request.ncNumber()) || !sameId(quote.getNcId(), request.ncId())) {
            throw new QuoteValidationException("ncNumber cannot be changed");
        }
    }

    private static boolean sameNc(String left, String right) {
        return ncText(left).equalsIgnoreCase(ncText(right));
    }

    private static boolean sameId(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }

    private static Map<String, Integer> ncCounts(List<Quote> quotes) {
        Map<String, Integer> counts = new HashMap<>();
        for (Quote quote : quotes) {
            String nc = ncText(quote.getNcNumber()).toLowerCase(Locale.ROOT);
            if (!nc.isEmpty()) {
                counts.merge(nc, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static int ncSize(Quote quote, Map<String, Integer> ncCounts) {
        String nc = ncText(quote.getNcNumber()).toLowerCase(Locale.ROOT);
        if (nc.isEmpty()) {
            return 1;
        }
        return ncCounts.getOrDefault(nc, 1);
    }

    private int ncSize(Quote quote) {
        String nc = ncText(quote.getNcNumber());
        if (nc.isEmpty()) {
            return 1;
        }
        return Math.max(1, quoteRepository.findByNcNumberIgnoreCaseAndIsDeletedFalse(nc).size());
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
        applyContacts(quote, request);
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
        applyQuotePersons(quote, request);
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
        applyFai(quote, request);
    }

    private static void applyFai(Quote quote, QuoteRequest request) {
        boolean reqPnc = isTrue(request.faiReqPnc());
        boolean reqAs9102 = isTrue(request.faiReqAs9102());
        quote.setFaiReqPnc(reqPnc);
        quote.setFaiPcbWo(optionalFaiPrice(request.faiPcbWo(), reqPnc, "PNC FAI PCB"));
        quote.setFaiPcbaWo(optionalFaiPrice(request.faiPcbaWo(), reqPnc, "PNC FAI PCBA"));
        quote.setFaiHeadingPnc(blankToNull(request.faiHeadingPnc()));
        quote.setFaiReqAs9102(reqAs9102);
        quote.setFaiPcbWith(optionalFaiPrice(request.faiPcbWith(), reqAs9102, "AS9102 FAI PCB"));
        quote.setFaiPcbaWith(optionalFaiPrice(request.faiPcbaWith(), reqAs9102, "AS9102 FAI PCBA"));
        quote.setFaiHeadingAs9102(blankToNull(request.faiHeadingAs9102()));
    }

    private static BigDecimal optionalFaiPrice(BigDecimal value, boolean required, String field) {
        if (required && value == null) {
            throw new QuoteValidationException(field + " is required");
        }
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new QuoteValidationException(field + " cannot be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * A null person leaves the stored name. A blank value clears it.
     */
    private static void applyQuotePersons(Quote quote, QuoteRequest request) {
        if (request.assyQuotePerson() != null) {
            quote.setAssyQuotePerson(blankToNull(request.assyQuotePerson()));
        }
        if (request.pcbQuotePerson() != null) {
            quote.setPcbQuotePerson(blankToNull(request.pcbQuotePerson()));
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
        quantity.setServiceCharge(row.serviceCharge());
        quantity.setCommissionPercentage(row.commissionPercentage());
        quantity.setPcbNre(row.pcbNre());
        quantity.setAssyNre(row.assyNre());
        quantity.setStencil(row.stencil());
        quantity.setOtherNre(row.otherNre());
        quantity.setComments(blankToNull(row.comments()));
        quantity.setReceived(isTrue(row.received()));
        // subTotal, totalSalesPct and total are derived, so whatever the client
        // posted for them is discarded in favour of the numbers below.
        recalculate(quantity);
    }

    /**
     * Fills in the three derived amounts. Each one is rounded before it feeds the
     * next, so the figures on screen add up when someone checks them by hand.
     * The row's own commission percentage is what lifts the sub total.
     */
    private static void recalculate(QuoteQuantity quantity) {
        BigDecimal partsNet = plusPercent(quantity.getPartsCost(), quantity.getPartMarkup());
        BigDecimal laborNet = plusPercent(quantity.getLaborCost(), negate(quantity.getLaborDiscount()));
        BigDecimal subTotal = money(zeroIfNull(quantity.getPcbCost())
                .add(partsNet)
                .add(laborNet)
                .add(zeroIfNull(quantity.getTesting()))
                .add(zeroIfNull(quantity.getConfCoat()))
                .add(zeroIfNull(quantity.getServiceCharge())));
        BigDecimal totalSalesPct = money(plusPercent(subTotal, quantity.getCommissionPercentage()));
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
                contactIdSignature(quote),
                quote.getCustomerRfq(), quote.getNcId(), quote.getNcNumber(),
                quote.getAssyNumber(), quote.getPcbaRevision(), quote.getPcbNumber(), quote.getPcbRevision(),
                quote.getAssyQuoteStatus(), quote.getPcbQuoteNumber(), quote.getPcbQuoteStatus(),
                quote.getQuoteArray(), norm(quote.getCommissionPercentage()),
                quote.getInternalNote1(), quote.getInternalNote2(), quote.getNotesToCustomer(),
                quote.getOtherNreCharges(), quote.isPncNotes(), quote.isLaborOnly(),
                quote.isPartsScheduled(), quote.isFeedback(), quote.isItarc(), quote.isBerryc(),
                quote.isSamsReview(), quote.getPcbaPlant(), quote.getPcbOrigin(), quote.getStatus(),
                quote.isFaiReqPnc(), norm(quote.getFaiPcbWo()), norm(quote.getFaiPcbaWo()),
                quote.getFaiHeadingPnc(), quote.isFaiReqAs9102(),
                norm(quote.getFaiPcbWith()), norm(quote.getFaiPcbaWith()), quote.getFaiHeadingAs9102()
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

    private Map<Long, BigDecimal> receivedTotals(List<Long> qids) {
        if (qids.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = quoteRepository.sumReceivedTotals(qids);
        if (rows == null) {
            return Map.of();
        }
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] instanceof Number qid && row[1] instanceof BigDecimal total) {
                totals.put(qid.longValue(), total);
            }
        }
        return totals;
    }

    private QuoteSummaryResponse toSummary(Quote quote, QuoteLockResponse lock, int familySize, BigDecimal receivedTotal) {
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
                quote.getPcbaRevision(),
                quote.getPcbNumber(),
                quote.getPcbRevision(),
                quote.getAssyQuoteStatus(),
                quote.getPcbQuoteStatus(),
                quote.getStatus(),
                quote.isItarc(),
                quote.isBerryc(),
                quote.isSamsReview(),
                lock,
                familySize,
                quote.getCustId(),
                receivedTotal
        );
    }

    private QuoteResponse toResponse(Quote quote, Long userId, boolean includeAudit) {
        return toResponse(quote, userId, includeAudit, ncSize(quote));
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
                quote.isFaiReqPnc(),
                quote.getFaiPcbWo(),
                quote.getFaiPcbaWo(),
                quote.getFaiHeadingPnc(),
                quote.isFaiReqAs9102(),
                quote.getFaiPcbWith(),
                quote.getFaiPcbaWith(),
                quote.getFaiHeadingAs9102(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getQuantities().stream().map(QuoteService::toQuantityResponse).toList(),
                includeAudit
                        ? historyRepository.findAllByQidOrderByChangedAtDesc(quote.getQid()).stream()
                                .map(QuoteService::toHistoryResponse).toList()
                        : List.of(),
                includeAudit ? lockService.find(quote.getQid(), userId) : null,
                familySize,
                attachmentRepository.findCategories(quote.getQid()),
                toContactResponses(quote)
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
                quantity.getCommissionPercentage(),
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

    /**
     * A contact list replaces the rows. A null list keeps the older single contId.
     * cont_id and contact_name always follow the primary contact.
     * Rows that stay selected are updated in place so the primary key is not deleted and inserted again.
     */
    private void applyContacts(Quote quote, QuoteRequest request) {
        if (request.contacts() == null) {
            applyLegacyContact(quote, request.contId(), request.contactName());
            return;
        }
        List<QuoteContactRequest> rows = request.contacts();
        Map<Long, Contact> byId = requireQuoteContacts(request.custId(), rows);
        String primaryName = null;
        for (QuoteContactRequest row : rows) {
            if (row.primary()) {
                primaryName = contactLabel(byId.get(row.contId()));
            }
        }
        writeContacts(quote, rows, primaryName);
    }

    /** Clients that still send only contId get one primary row. */
    private void applyLegacyContact(Quote quote, Long contId, String contactName) {
        List<QuoteContactRequest> rows = contId == null
                ? List.of()
                : List.of(new QuoteContactRequest(contId, true));
        writeContacts(quote, rows, resolveContactName(contId, contactName));
    }

    private static void writeContacts(Quote quote, List<QuoteContactRequest> rows, String primaryName) {
        Set<Long> kept = new HashSet<>();
        for (QuoteContactRequest row : rows) {
            kept.add(row.contId());
        }
        quote.getContacts().removeIf(link -> link.getContId() == null || !kept.contains(link.getContId()));
        Map<Long, QuoteContact> remaining = new HashMap<>();
        for (QuoteContact link : quote.getContacts()) {
            remaining.put(link.getContId(), link);
        }
        QuoteContactRequest primary = null;
        for (QuoteContactRequest row : rows) {
            QuoteContact link = remaining.get(row.contId());
            if (link == null) {
                link = new QuoteContact();
                link.setContId(row.contId());
                link.setQuote(quote);
                quote.getContacts().add(link);
            }
            link.setPrimaryContact(row.primary());
            if (row.primary()) {
                primary = row;
            }
        }
        if (primary == null) {
            quote.setContId(null);
            quote.setContactName(null);
            return;
        }
        quote.setContId(primary.contId());
        quote.setContactName(primaryName);
    }

    private Map<Long, Contact> requireQuoteContacts(Long custId, List<QuoteContactRequest> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        if (custId == null) {
            throw new QuoteValidationException("Choose a customer before selecting contacts.");
        }
        Set<Long> seen = new HashSet<>();
        int primaries = 0;
        List<Long> ids = new ArrayList<>();
        for (QuoteContactRequest row : rows) {
            if (row == null || row.contId() == null) {
                throw new QuoteValidationException("contId is required");
            }
            if (!seen.add(row.contId())) {
                throw new QuoteValidationException("Each contact can be selected once.");
            }
            if (row.primary()) {
                primaries++;
            }
            ids.add(row.contId());
        }
        if (primaries != 1) {
            throw new QuoteValidationException(primaries == 0
                    ? "Select one primary contact."
                    : "A quote can have only one primary contact.");
        }
        Map<Long, Contact> byId = contactsById(ids);
        if (byId.size() != ids.size()) {
            throw new QuoteValidationException("A selected contact was not found.");
        }
        for (Contact contact : byId.values()) {
            Long owner = contact.getCustomer() == null ? null : contact.getCustomer().getCustId();
            if (!custId.equals(owner)) {
                throw new QuoteValidationException("Each contact must belong to the quote customer.");
            }
        }
        return byId;
    }

    /** Snapshot, so a later edit of the contact rows does not rewrite an earlier signature. */
    private static List<Long> contactIdSignature(Quote quote) {
        List<Long> ids = new ArrayList<>();
        if (quote.getContacts() != null && !quote.getContacts().isEmpty()) {
            for (QuoteContact link : quote.getContacts()) {
                if (link.getContId() != null) {
                    ids.add(link.getContId());
                }
            }
        } else if (quote.getContId() != null) {
            ids.add(quote.getContId());
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    private List<QuoteContactResponse> toContactResponses(Quote quote) {
        List<QuoteContact> links = quote.getContacts() == null ? List.of() : new ArrayList<>(quote.getContacts());
        if (links.isEmpty()) {
            if (quote.getContId() == null) {
                return List.of();
            }
            Contact contact = contactRepository.findById(quote.getContId()).orElse(null);
            return List.of(new QuoteContactResponse(
                    quote.getContId(),
                    displayedContactName(quote),
                    contact == null ? null : blankToNull(contact.getEmail()),
                    true));
        }
        Map<Long, Contact> byId = contactsById(links.stream().map(QuoteContact::getContId).toList());
        links.sort(Comparator
                .comparing(QuoteContact::isPrimaryContact).reversed()
                .thenComparing(QuoteContact::getContId, Comparator.nullsLast(Comparator.naturalOrder())));
        List<QuoteContactResponse> responses = new ArrayList<>();
        for (QuoteContact link : links) {
            Contact contact = byId.get(link.getContId());
            String name = contact == null ? null : contactLabel(contact);
            if (link.isPrimaryContact()) {
                String stored = displayedContactName(quote);
                if (stored != null) {
                    name = stored;
                }
            }
            if (name == null) {
                name = "Contact #" + link.getContId();
            }
            String email = contact == null ? null : blankToNull(contact.getEmail());
            responses.add(new QuoteContactResponse(link.getContId(), name, email, link.isPrimaryContact()));
        }
        return responses;
    }

    private Map<Long, Contact> contactsById(List<Long> ids) {
        List<Contact> found = contactRepository.findAllById(ids);
        Map<Long, Contact> byId = new HashMap<>();
        if (found == null) {
            return byId;
        }
        for (Contact contact : found) {
            if (contact.getContId() != null) {
                byId.put(contact.getContId(), contact);
            }
        }
        return byId;
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
