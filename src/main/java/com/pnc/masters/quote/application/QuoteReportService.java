package com.pnc.masters.quote.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteReportMonthResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class QuoteReportService {

    private static final String CANCELLED = "cancelled";
    private static final String RECEIVED = "received";
    private static final String PCB_ONLY = "pcb only";
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
            if (inYear(quote.getCreateDate(), year)) {
                int index = quote.getCreateDate().getMonthValue() - 1;
                quoteCounts[index]++;
                BigDecimal smallest = extremeTotal(quote, true);
                BigDecimal largest = extremeTotal(quote, false);
                allSmallest[index] = allSmallest[index].add(smallest);
                allLargest[index] = allLargest[index].add(largest);
                if (isNewCustomer(quote, newCustomerIds.get(index))) {
                    newSmallest[index] = newSmallest[index].add(smallest);
                    newLargest[index] = newLargest[index].add(largest);
                }
            }
            if (inYear(quote.getReceivedDate(), year)) {
                int index = quote.getReceivedDate().getMonthValue() - 1;
                orderCounts[index]++;
                if (received(quote)) {
                    BigDecimal won = receivedTotal(quote);
                    allWon[index] = allWon[index].add(won);
                    pcbRevenue[index] = pcbRevenue[index].add(pcbRevenue(quote));
                    if (isNewCustomer(quote, newCustomerIds.get(index))) {
                        newWon[index] = newWon[index].add(won);
                    }
                }
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
        String status = quote.getStatus() == null ? "" : quote.getStatus().trim().toLowerCase(Locale.ROOT);
        if (CANCELLED.equals(status)) {
            return true;
        }
        String type = quote.getQuoteType() == null ? "" : quote.getQuoteType().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return type.contains(PCB_ONLY);
    }

    private static boolean received(Quote quote) {
        return quote.getStatus() != null && RECEIVED.equals(quote.getStatus().trim().toLowerCase(Locale.ROOT));
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

    /** Smallest qty when {@code smallest} is true, otherwise the largest. Ties add together. */
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
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteQuantity row : rows) {
            if (row.getQty() == extreme) {
                sum = sum.add(zero(row.getTotal()));
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
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
            sum = sum.add(unit.multiply(BigDecimal.valueOf(qty)));
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
