package com.pnc.masters.quote.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteQuantity;
import com.pnc.masters.quote.api.CustomerQuoteYearResponse;
import com.pnc.masters.quote.api.QuoteReportMonthResponse;
import com.pnc.masters.quote.api.ReferredQuoteSummaryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteReportServiceTest {

    @Test
    void bucketsAYearAndKeepsPcbOnlyQuotes() {
        Customer januaryCustomer = customer(4L, LocalDateTime.of(2026, 1, 10, 9, 0));
        Quote quoted = quote(1L, "Turnkey", "open", LocalDate.of(2026, 1, 5), null, 4L, "5.00");
        quoted.setSubmitDate(LocalDate.of(2026, 1, 6));
        quoted.addQuantity(row(10, "100.00", false, "2.00"));
        quoted.addQuantity(row(10, "50.00", false, "1.00"));
        quoted.addQuantity(row(40, "400.00", false, "3.00"));

        Quote won = quote(2L, "Turnkey", "received", LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 20), 4L, "10.00");
        won.setSubmitDate(LocalDate.of(2026, 1, 18));
        won.addQuantity(row(5, "20.00", false, "8.00", "999.00"));
        won.addQuantity(row(25, "80.00", true, "10.00", "15.00"));

        Quote cancelled = quote(3L, "Turnkey", "cancelled", LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9), 9L, "10.00");
        cancelled.addQuantity(row(1, "999.00", true, "50.00"));

        Quote pcbOnly = quote(4L, "R-PCB Only", "received", LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 15), 4L, "10.00");
        pcbOnly.setSubmitDate(LocalDate.of(2026, 2, 2));
        pcbOnly.addQuantity(row(2, "500.00", true, "20.00", "6.00"));

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
        assertThat(january.allSmallest()).isEqualByComparingTo("20.00");
        assertThat(january.allLargest()).isEqualByComparingTo("80.00");
        assertThat(january.allWon()).isEqualByComparingTo("80.00");
        assertThat(january.newSmallest()).isEqualByComparingTo("20.00");
        assertThat(january.newLargest()).isEqualByComparingTo("80.00");
        assertThat(january.newWon()).isEqualByComparingTo("80.00");
        assertThat(january.pcbRevenue()).isEqualByComparingTo("340.00");
        assertThat(months).hasSize(12);
        QuoteReportMonthResponse february = months.get(1);
        assertThat(february.quotes()).isEqualTo(1);
        assertThat(february.orders()).isEqualTo(1);
        assertThat(february.allSmallest()).isEqualByComparingTo("500.00");
        assertThat(february.allLargest()).isEqualByComparingTo("500.00");
        assertThat(february.allWon()).isEqualByComparingTo("500.00");
        assertThat(february.newWon()).isZero();
        assertThat(february.pcbRevenue()).isZero();
    }

    @Test
    void smallestAndLargestFollowSubmitDate() {
        Customer februaryCustomer = customer(4L, LocalDateTime.of(2026, 2, 3, 9, 0));
        Quote quote = quote(5L, "Turnkey", "submitted", LocalDate.of(2026, 1, 5), null, 4L, "5.00");
        quote.setSubmitDate(LocalDate.of(2026, 2, 12));
        quote.addQuantity(row(10, "100.00", false, "2.00"));
        quote.addQuantity(row(40, "400.00", false, "3.00"));

        List<QuoteReportMonthResponse> months = QuoteReportService.months(
                2026,
                List.of(quote),
                List.of(februaryCustomer)
        );

        QuoteReportMonthResponse january = months.get(0);
        assertThat(january.quotes()).isZero();
        assertThat(january.allSmallest()).isZero();
        assertThat(january.allLargest()).isZero();
        assertThat(january.newSmallest()).isZero();
        assertThat(january.newLargest()).isZero();

        QuoteReportMonthResponse february = months.get(1);
        assertThat(february.quotes()).isEqualTo(1);
        assertThat(february.orders()).isZero();
        assertThat(february.allSmallest()).isEqualByComparingTo("100.00");
        assertThat(february.allLargest()).isEqualByComparingTo("400.00");
        assertThat(february.newSmallest()).isEqualByComparingTo("100.00");
        assertThat(february.newLargest()).isEqualByComparingTo("400.00");
    }

    @Test
    void tiedQuantityKeepsTheSmallerSmallestAndLargerLargest() {
        Customer januaryCustomer = customer(4L, LocalDateTime.of(2026, 1, 4, 9, 0));
        Quote tied = quote(10L, "Turnkey", "submitted", LocalDate.of(2026, 1, 2), null, 4L, "0");
        tied.setSubmitDate(LocalDate.of(2026, 1, 8));
        tied.addQuantity(row(10, "100.00", false, "1.00"));
        tied.addQuantity(row(10, "50.00", false, "1.00"));
        tied.addQuantity(row(40, "400.00", false, "1.00"));
        tied.addQuantity(row(40, "250.00", false, "1.00"));

        Quote other = quote(11L, "Turnkey", "received", LocalDate.of(2026, 1, 3), null, 4L, "0");
        other.setSubmitDate(LocalDate.of(2026, 1, 9));
        other.addQuantity(row(5, "20.00", false, "1.00"));
        other.addQuantity(row(25, "80.00", true, "1.00"));

        List<QuoteReportMonthResponse> months = QuoteReportService.months(
                2026,
                List.of(tied, other),
                List.of(januaryCustomer)
        );

        QuoteReportMonthResponse january = months.get(0);
        assertThat(january.allSmallest()).isEqualByComparingTo("70.00");
        assertThat(january.allLargest()).isEqualByComparingTo("480.00");
        assertThat(january.newSmallest()).isEqualByComparingTo("70.00");
        assertThat(january.newLargest()).isEqualByComparingTo("480.00");
    }

    @Test
    void customerYearsMatchIdOrNameAndKeepTheTiedExtremeTotal() {
        Quote named = quote(1L, "Turnkey", "submitted", LocalDate.of(2026, 1, 2), null, null, "0");
        named.setSubmitDate(LocalDate.of(2026, 3, 1));
        named.setCustomerName(" acme ");
        named.addQuantity(row(10, "100.00", false, "1.00"));
        named.addQuantity(row(10, "50.00", false, "1.00"));
        named.addQuantity(row(40, "400.00", false, "1.00"));
        named.addQuantity(row(40, "250.00", false, "1.00"));

        Quote byId = quote(2L, "Turnkey", "received", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 1), 4L, "0");
        byId.setSubmitDate(LocalDate.of(2026, 2, 2));
        byId.addQuantity(row(5, "20.00", false, "1.00"));
        byId.addQuantity(row(25, "80.00", true, "1.00"));

        Quote otherIdSameName = quote(3L, "Turnkey", "received", LocalDate.of(2026, 1, 1), null, 9L, "0");
        otherIdSameName.setSubmitDate(LocalDate.of(2026, 1, 5));
        otherIdSameName.setCustomerName("Acme");
        otherIdSameName.addQuantity(row(1, "999.00", true, "1.00"));

        Quote otherName = quote(4L, "Turnkey", "submitted", LocalDate.of(2026, 1, 1), null, null, "0");
        otherName.setSubmitDate(LocalDate.of(2026, 1, 6));
        otherName.setCustomerName("Other");
        otherName.addQuantity(row(3, "30.00", false, "1.00"));

        List<CustomerQuoteYearResponse> years = QuoteReportService.customerYears(
                2026, 4L, "Acme", List.of(named, byId, otherIdSameName, otherName));

        CustomerQuoteYearResponse current = years.get(0);
        assertThat(years).extracting(CustomerQuoteYearResponse::year).containsExactly(2026, 2025, 2024, 2023);
        assertThat(current.quotes()).isEqualTo(2);
        assertThat(current.received()).isEqualTo(1);
        assertThat(current.winRatio()).isEqualTo(50);
        assertThat(current.smallest()).isEqualByComparingTo("70.00");
        assertThat(current.largest()).isEqualByComparingTo("480.00");
        assertThat(current.won()).isEqualByComparingTo("80.00");
        assertThat(years.get(1).quotes()).isZero();
    }

    @Test
    void referredCountsGroupSubmittedAndReceivedQuotesByTag() {
        Customer acme = customer(4L, LocalDateTime.of(2024, 1, 1, 0, 0));
        acme.setCustomer("Acme");
        acme.setReferredBy("Alice, Bob");
        Customer other = customer(9L, LocalDateTime.of(2024, 1, 1, 0, 0));
        other.setCustomer("Other");
        other.setReferredBy("Alice");
        Customer sharedA = customer(2L, LocalDateTime.of(2024, 1, 1, 0, 0));
        sharedA.setCustomer("Shared");
        sharedA.setReferredBy("Nope");
        Customer sharedB = customer(3L, LocalDateTime.of(2024, 1, 1, 0, 0));
        sharedB.setCustomer(" shared ");
        sharedB.setReferredBy("Nope");

        Quote submitted = quote(1L, "Turnkey", "submitted", LocalDate.of(2026, 1, 2), null, 4L, "0");
        submitted.setSubmitDate(LocalDate.of(2026, 3, 1));
        Quote received = quote(2L, "Turnkey", "received", LocalDate.of(2025, 1, 2), null, 9L, "0");
        received.setSubmitDate(LocalDate.of(2025, 6, 1));
        Quote open = quote(3L, "Turnkey", "open", LocalDate.of(2026, 1, 2), null, 4L, "0");
        open.setSubmitDate(LocalDate.of(2026, 4, 1));
        Quote cancelled = quote(4L, "Turnkey", "cancelled", LocalDate.of(2026, 1, 2), null, 4L, "0");
        cancelled.setSubmitDate(LocalDate.of(2026, 5, 1));
        Quote byName = quote(5L, "Turnkey", "submitted", LocalDate.of(2024, 1, 2), null, null, "0");
        byName.setSubmitDate(LocalDate.of(2024, 2, 1));
        byName.setCustomerName(" acme ");
        Quote otherIdSameName = quote(6L, "Turnkey", "submitted", LocalDate.of(2026, 1, 2), null, 9L, "0");
        otherIdSameName.setSubmitDate(LocalDate.of(2026, 7, 1));
        otherIdSameName.setCustomerName("Acme");
        Quote sharedName = quote(7L, "Turnkey", "submitted", LocalDate.of(2026, 1, 2), null, null, "0");
        sharedName.setSubmitDate(LocalDate.of(2026, 8, 1));
        sharedName.setCustomerName("Shared");
        Quote tooOld = quote(8L, "Turnkey", "submitted", LocalDate.of(2020, 1, 2), null, 4L, "0");
        tooOld.setSubmitDate(LocalDate.of(2020, 1, 2));

        List<ReferredQuoteSummaryResponse> rows = QuoteReportService.referredCounts(
                2026,
                List.of(submitted, received, open, cancelled, byName, otherIdSameName, sharedName, tooOld),
                List.of(acme, other, sharedA, sharedB)
        );

        assertThat(rows).extracting(ReferredQuoteSummaryResponse::referredBy).containsExactly("Alice", "Bob");
        assertThat(rows.get(0).years()).extracting(year -> year.year()).containsExactly(2026, 2025, 2024, 2023, 2022);
        assertThat(rows.get(0).years()).extracting(year -> year.quotes()).containsExactly(2L, 1L, 1L, 0L, 0L);
        assertThat(rows.get(1).years()).extracting(year -> year.quotes()).containsExactly(1L, 0L, 1L, 0L, 0L);
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
        return row(qty, total, received, pcbCost, null);
    }

    private static QuoteQuantity row(int qty, String total, boolean received, String pcbCost, String pcbNre) {
        QuoteQuantity row = new QuoteQuantity();
        row.setQty(qty);
        row.setTotal(new BigDecimal(total));
        row.setReceived(received);
        row.setPcbCost(new BigDecimal(pcbCost));
        if (pcbNre != null) {
            row.setPcbNre(new BigDecimal(pcbNre));
        }
        return row;
    }
}
