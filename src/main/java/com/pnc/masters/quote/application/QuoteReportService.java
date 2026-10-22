package com.pnc.masters.quote.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.CustomerQuoteYearResponse;
import com.pnc.masters.quote.api.QuoteReportMonthResponse;
import com.pnc.masters.quote.api.ReferredQuoteSummaryResponse;
import com.pnc.masters.quote.api.ReferredQuoteYearCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class QuoteReportService implements CustomerWinRatio {

    private static final String CANCELLED = "cancelled";
    private static final String SUBMITTED = "submitted";
    private static final String RECEIVED = "received";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final QuoteRepository quoteRepository;
    private final CustomerRepository customerRepository;

    public QuoteReportService(QuoteRepository quoteRepository, CustomerRepository customerRepository) {
        this.quoteRepository = quoteRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<QuoteReportMonthResponse> report(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = start.plusYears(1);
        List<Quote> quotes = quoteRepository.findForReport(start, end);
        List<Customer> customers = customerRepository.findAddedBetween(start.atStartOfDay(), end.atStartOfDay());
        return months(year, quotes, customers);
    }

    /** Same current-year win ratio the customer history dialog shows in its first column. */
    @Transactional(readOnly = true)
    @Override
    public int currentYear(Long custId, String customerName) {
        int year = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(year, 1, 1);
        List<Quote> quotes = quoteRepository.findSubmittedBetween(start, start.plusYears(1));
        return customerYears(year, custId, customerName, quotes).get(0).winRatio();
    }

    /** The current year and the three before it, newest first, for one customer. */
    @Transactional(readOnly = true)
    public List<CustomerQuoteYearResponse> customerSummary(Long custId) {
        Customer customer = customerRepository.findByCustIdAndIsDeletedFalse(custId)
                .orElseThrow(() -> new CustomerNotFoundException(custId));
        int endYear = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(endYear - 3, 1, 1);
        LocalDate end = start.plusYears(4);
        List<Quote> quotes = quoteRepository.findSubmittedBetween(start, end);
        return customerYears(endYear, custId, customer.getCustomer(), quotes);
    }

    /** Quote counts for the current year and the four before it, one row per referred-by tag. */
    @Transactional(readOnly = true)
    public List<ReferredQuoteSummaryResponse> referredSummary() {
        int endYear = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(endYear - 4, 1, 1);
        LocalDate end = start.plusYears(5);
        List<Quote> quotes = quoteRepository.findSubmittedHeadersBetween(start, end);
        List<Customer> customers = customerRepository.findAllByIsDeletedFalse();
        return referredCounts(endYear, quotes, customers);
    }

    static List<ReferredQuoteSummaryResponse> referredCounts(int endYear, List<Quote> quotes, List<Customer> customers) {
        int span = 5;
        Map<Long, Customer> byId = new HashMap<>();
        Map<String, List<Customer>> byName = new HashMap<>();
        for (Customer customer : customers) {
            if (customer.getCustId() != null) {
                byId.put(customer.getCustId(), customer);
            }
            String name = normalize(customer.getCustomer());
            if (!name.isEmpty()) {
                byName.computeIfAbsent(name, key -> new ArrayList<>()).add(customer);
            }
        }

        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (Quote quote : quotes) {
            if (excluded(quote) || !inYear(quote.getSubmitDate(), endYear, span)) {
                continue;
            }
            if (!submitted(quote) && !received(quote)) {
                continue;
            }
            Customer customer = customerFor(quote, byId, byName);
            if (customer == null) {
                continue;
            }
            int index = endYear - quote.getSubmitDate().getYear();
            for (String tag : referralTags(customer.getReferredBy())) {
                String key = normalize(tag);
                labels.putIfAbsent(key, tag);
                counts.computeIfAbsent(key, ignored -> new long[span])[index]++;
            }
        }

        List<String> keys = new ArrayList<>(counts.keySet());
        keys.sort(Comparator.comparing(labels::get, String.CASE_INSENSITIVE_ORDER));
        List<ReferredQuoteSummaryResponse> rows = new ArrayList<>();
        for (String key : keys) {
            long[] row = counts.get(key);
            List<ReferredQuoteYearCount> years = new ArrayList<>();
            for (int index = 0; index < span; index++) {
                years.add(new ReferredQuoteYearCount(endYear - index, row[index]));
            }
            rows.add(new ReferredQuoteSummaryResponse(labels.get(key), years));
        }
        return rows;
    }

    private static Customer customerFor(Quote quote, Map<Long, Customer> byId, Map<String, List<Customer>> byName) {
        if (quote.getCustId() != null) {
            return byId.get(quote.getCustId());
        }
        List<Customer> named = byName.get(normalize(quote.getCustomerName()));
        return named != null && named.size() == 1 ? named.get(0) : null;
    }

    private static List<String> referralTags(String referredBy) {
        if (referredBy == null || referredBy.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String part : referredBy.split(",")) {
            String tag = part.trim();
            if (!tag.isEmpty() && seen.add(normalize(tag))) {
                tags.add(tag);
            }
        }
        return tags;
    }

    static List<CustomerQuoteYearResponse> customerYears(int endYear, Long custId, String customerName, List<Quote> quotes) {
        int span = 4;
        long[] quoteCounts = new long[span];
        long[] receivedCounts = new long[span];
        BigDecimal[] smallest = zeros();
        BigDecimal[] largest = zeros();
        BigDecimal[] won = zeros();
        String name = normalize(customerName);

        for (Quote quote : quotes) {
            if (excluded(quote) || !belongsToCustomer(quote, custId, name) || !inYear(quote.getSubmitDate(), endYear, span)) {
                continue;
            }
            int index = endYear - quote.getSubmitDate().getYear();
            if (submitted(quote) || received(quote)) {
                quoteCounts[index]++;
                smallest[index] = smallest[index].add(extremeTotal(quote, true));
                largest[index] = largest[index].add(extremeTotal(quote, false));
                won[index] = won[index].add(receivedTotal(quote));
            }
            if (received(quote)) {
                receivedCounts[index]++;
            }
        }

        List<CustomerQuoteYearResponse> rows = new ArrayList<>();
        for (int index = 0; index < span; index++) {
            rows.add(new CustomerQuoteYearResponse(
                    endYear - index,
                    quoteCounts[index],
                    receivedCounts[index],
                    winRatio(quoteCounts[index], receivedCounts[index]),
                    smallest[index],
                    largest[index],
                    won[index]
            ));
        }
        return rows;
    }

    private static boolean inYear(LocalDate date, int endYear, int span) {
        return date != null && date.getYear() <= endYear && date.getYear() > endYear - span;
    }

    private static boolean belongsToCustomer(Quote quote, Long custId, String normalizedName) {
        if (custId != null && custId.equals(quote.getCustId())) {
            return true;
        }
        if (quote.getCustId() != null || normalizedName.isEmpty()) {
            return false;
        }
        return normalizedName.equals(normalize(quote.getCustomerName()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static List<QuoteReportMonthResponse> months(int year, List<Quote> quotes, List<Customer> customers) {
        List<Set<Long>> newCustomerIds = new ArrayList<>();
        long[] newCustomers = new long[12];
        for (int month = 0; month < 12; month++) {
            newCustomerIds.add(new HashSet<>());
        }
        for (Customer customer : customers) {
            LocalDateTime entered = customer.getCustEntryDt();
            if (entered == null || entered.getYear() != year || customer.getCustId() == null) {
                continue;
            }
            int index = entered.getMonthValue() - 1;
            newCustomers[index]++;
            newCustomerIds.get(index).add(customer.getCustId());
        }

        long[] quoteCounts = new long[12];
        long[] orderCounts = new long[12];
        BigDecimal[] allSmallest = zeros();
        BigDecimal[] allLargest = zeros();
        BigDecimal[] allWon = zeros();
        BigDecimal[] newSmallest = zeros();
        BigDecimal[] newLargest = zeros();
        BigDecimal[] newWon = zeros();
        BigDecimal[] pcbRevenue = zeros();

        for (Quote quote : quotes) {
            if (excluded(quote)) {
                continue;
            }
            if (inYear(quote.getSubmitDate(), year)) {
                int index = quote.getSubmitDate().getMonthValue() - 1;
                if (submitted(quote) || received(quote)) {
                    quoteCounts[index]++;
                    BigDecimal smallest = extremeTotal(quote, true);
                    BigDecimal largest = extremeTotal(quote, false);
                    BigDecimal won = receivedTotal(quote);
                    allSmallest[index] = allSmallest[index].add(smallest);
                    allLargest[index] = allLargest[index].add(largest);
                    allWon[index] = allWon[index].add(won);
                    if (isNewCustomer(quote, newCustomerIds.get(index))) {
                        newSmallest[index] = newSmallest[index].add(smallest);
                        newLargest[index] = newLargest[index].add(largest);
                        newWon[index] = newWon[index].add(won);
                    }
                }
                if (received(quote)) {
                    orderCounts[index]++;
                }
            }
            if (inYear(quote.getReceivedDate(), year) && received(quote)) {
                int index = quote.getReceivedDate().getMonthValue() - 1;
                pcbRevenue[index] = pcbRevenue[index].add(pcbRevenue(quote));
            }
        }

        List<QuoteReportMonthResponse> rows = new ArrayList<>();
        for (int month = 0; month < 12; month++) {
            rows.add(new QuoteReportMonthResponse(
                    month + 1,
                    quoteCounts[month],
                    orderCounts[month],
                    winRatio(quoteCounts[month], orderCounts[month]),
                    newCustomers[month],
                    allSmallest[month],
                    allLargest[month],
                    allWon[month],
                    newSmallest[month],
                    newLargest[month],
                    newWon[month],
                    pcbRevenue[month]
            ));
        }
        return rows;
    }

    private static boolean excluded(Quote quote) {
        return isStatus(quote, CANCELLED);
    }

    private static boolean submitted(Quote quote) {
        return isStatus(quote, SUBMITTED);
    }

    private static boolean received(Quote quote) {
        return isStatus(quote, RECEIVED);
    }

    private static boolean isStatus(Quote quote, String expected) {
        return quote.getStatus() != null && expected.equals(quote.getStatus().trim().toLowerCase(Locale.ROOT));
    }

    private static boolean inYear(LocalDate date, int year) {
        return date != null && date.getYear() == year;
    }

    private static boolean isNewCustomer(Quote quote, Set<Long> ids) {
        return quote.getCustId() != null && ids.contains(quote.getCustId());
    }

    private static int winRatio(long quotes, long orders) {
        if (quotes == 0) {
            return 0;
        }
        return (int) Math.round(orders * 100.0 / quotes);
    }

    /**
     * Smallest qty when {@code smallest} is true, otherwise the largest.
     * Rows that share that qty keep the smaller total for smallest, and the larger total for largest.
     */
    private static BigDecimal extremeTotal(Quote quote, boolean smallest) {
        List<QuoteQuantity> rows = quote.getQuantities().stream().filter(row -> row.getQty() != null).toList();
        if (rows.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int extreme = rows.get(0).getQty();
        for (QuoteQuantity row : rows) {
            int qty = row.getQty();
            if (smallest ? qty < extreme : qty > extreme) {
                extreme = qty;
            }
        }
        BigDecimal chosen = null;
        for (QuoteQuantity row : rows) {
            if (row.getQty() != extreme) {
                continue;
            }
            BigDecimal total = zero(row.getTotal());
            boolean better = chosen == null || (smallest ? total.compareTo(chosen) < 0 : total.compareTo(chosen) > 0);
            if (better) {
                chosen = total;
            }
        }
        return chosen.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal receivedTotal(Quote quote) {
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteQuantity row : quote.getQuantities()) {
            if (row.isReceived()) {
                sum = sum.add(zero(row.getTotal()));
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal pcbRevenue(Quote quote) {
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteQuantity row : quote.getQuantities()) {
            if (!row.isReceived()) {
                continue;
            }
            BigDecimal unit = money(plusPercent(row.getPcbCost(), quote.getCommissionPercentage()));
            int qty = row.getQty() == null ? 0 : row.getQty();
            sum = sum.add(unit.multiply(BigDecimal.valueOf(qty))).add(zero(row.getPcbNre()));
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal plusPercent(BigDecimal amount, BigDecimal percent) {
        BigDecimal base = zero(amount);
        if (percent == null || percent.signum() == 0) {
            return base;
        }
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(HUNDRED, 4, RoundingMode.HALF_UP));
        return base.multiply(factor);
    }

    private static BigDecimal money(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] values = new BigDecimal[12];
        for (int i = 0; i < values.length; i++) {
            values[i] = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return values;
    }
}
