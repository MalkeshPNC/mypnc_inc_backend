package com.pnc.masters.quote.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.api.QuoteReportMonthResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteReportServiceTest {

    @Test
    void bucketsAYearAndSkipsCancelledAndPcbOnlyQuotes() {
        Customer januaryCustomer = customer(4L, LocalDateTime.of(2026, 1, 10, 9, 0));
        Quote quoted = quote(1L, "Turnkey", "open", LocalDate.of(2026, 1, 5), null, 4L, "5.00");
        quoted.addQuantity(row(10, "100.00", false, "2.00"));
        quoted.addQuantity(row(10, "50.00", false, "1.00"));
        quoted.addQuantity(row(40, "400.00", false, "3.00"));

        Quote won = quote(2L, "Turnkey", "received", LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 20), 4L, "10.00");
        won.addQuantity(row(5, "20.00", false, "8.00"));
        won.addQuantity(row(25, "80.00", true, "10.00"));

        Quote cancelled = quote(3L, "Turnkey", "cancelled", LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9), 9L, "10.00");
        cancelled.addQuantity(row(1, "999.00", true, "50.00"));

        Quote pcbOnly = quote(4L, "R-PCB Only", "received", LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 15), 4L, "10.00");
        pcbOnly.addQuantity(row(2, "500.00", true, "20.00"));

        List<QuoteReportMonthResponse> months = QuoteReportService.months(
                2026,
                List.of(quoted, won, cancelled, pcbOnly),
                List.of(januaryCustomer)
        );

        QuoteReportMonthResponse january = months.get(0);
        assertThat(january.quotes()).isEqualTo(1);
        assertThat(january.orders()).isEqualTo(1);
        assertThat(january.winRatio()).isEqualTo(100);
        assertThat(january.newCustomers()).isEqualTo(1);
        assertThat(january.allSmallest()).isEqualByComparingTo("150.00");
        assertThat(january.allLargest()).isEqualByComparingTo("400.00");
        assertThat(january.allWon()).isEqualByComparingTo("80.00");
        assertThat(january.newSmallest()).isEqualByComparingTo("150.00");
        assertThat(january.newLargest()).isEqualByComparingTo("400.00");
        assertThat(january.newWon()).isEqualByComparingTo("80.00");
        assertThat(january.pcbRevenue()).isEqualByComparingTo("275.00");
        assertThat(months).hasSize(12);
        assertThat(months.get(1).quotes()).isZero();
    }

    private static Customer customer(Long id, LocalDateTime entered) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCustEntryDt(entered);
        return customer;
    }

    private static Quote quote(Long id, String type, String status, LocalDate created, LocalDate received,
                               Long custId, String commission) {
        Quote quote = new Quote();
        quote.setQid(id);
        quote.setQuoteType(type);
        quote.setStatus(status);
        quote.setCreateDate(created);
        quote.setReceivedDate(received);
        quote.setCustId(custId);
        quote.setCommissionPercentage(new BigDecimal(commission));
        return quote;
    }

    private static QuoteQuantity row(int qty, String total, boolean received, String pcbCost) {
        QuoteQuantity row = new QuoteQuantity();
        row.setQty(qty);
        row.setTotal(new BigDecimal(total));
        row.setReceived(received);
        row.setPcbCost(new BigDecimal(pcbCost));
        return row;
    }
}
