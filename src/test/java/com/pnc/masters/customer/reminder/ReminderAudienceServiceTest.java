package com.pnc.masters.customer.reminder;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderCustomerResponse;
import com.pnc.masters.quote.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderAudienceServiceTest {

    @Mock
    private CustomerRepository customers;
    @Mock
    private ContactRepository contacts;
    @Mock
    private QuoteRepository quotes;

    private ReminderAudienceService service;

    @BeforeEach
    void setUp() {
        service = new ReminderAudienceService(customers, contacts, quotes);
        when(customers.findAllByIsDeletedFalseOrderByCustEntryDtDescCustIdDesc()).thenReturn(List.of(
                customer(1L, "Quoted"),
                customer(2L, "Ordered"),
                customer(3L, "Quiet"),
                customer(4L, "Line ordered"),
                customer(6L, "Pcb only")));
        when(quotes.findSubmittedFacts()).thenReturn(List.of(
                new Object[] {1L, 10L, LocalDate.of(2026, 1, 2), "Turnkey"},
                new Object[] {2L, 11L, LocalDate.of(2026, 1, 3), "PCB only"},
                new Object[] {4L, 9L, LocalDate.of(2025, 1, 1), "Turnkey"},
                new Object[] {4L, 12L, LocalDate.of(2026, 2, 1), "R - PCB only"},
                new Object[] {6L, 13L, LocalDate.of(2026, 3, 1), "PCB   ONLY"}));
        when(quotes.findCustomerIdsWithReceivedDate()).thenReturn(List.of(2L));
        when(quotes.findCustomerIdsWithReceivedLine()).thenReturn(List.of(4L));
        Contact contact = new Contact();
        contact.setContId(8L);
        contact.setFirstName("Pat");
        contact.setLastName("Lee");
        contact.setEmail("pat@acme.com");
        contact.setCustomer(customer(1L, "Quoted"));
        when(contacts.findWithCustomerByCustIdIn(any())).thenReturn(List.of(contact));
    }

    @Test
    void noLeadKeepsSubmittedCustomersWhoHaveNeverOrdered() {
        List<ReminderCustomerResponse> rows = service.audience(ReminderBatch.NO_LEAD).customers();

        assertThat(rows).extracting(ReminderCustomerResponse::custId).containsExactly(1L, 6L);
        ReminderCustomerResponse quoted = rows.get(0);
        assertThat(quoted.pcbOnly()).isFalse();
        assertThat(quoted.lastSubmitted()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(quoted.contacts()).hasSize(1);
        assertThat(quoted.contacts().get(0).email()).isEqualTo("pat@acme.com");
        assertThat(rows.get(1).pcbOnly()).isTrue();
        assertThat(rows).extracting(ReminderCustomerResponse::name).doesNotContain("Quiet", "Ordered", "Line ordered");
    }

    @Test
    void announcementsIncludeEveryNonDeletedCustomer() {
        List<ReminderCustomerResponse> rows = service.audience(ReminderBatch.ANNOUNCEMENT).customers();

        assertThat(rows).extracting(ReminderCustomerResponse::name)
                .containsExactly("Quoted", "Ordered", "Quiet", "Line ordered", "Pcb only");
        ReminderCustomerResponse lineOrdered = rows.get(3);
        assertThat(lineOrdered.lastSubmitted()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(lineOrdered.pcbOnly()).isTrue();
        assertThat(rows.get(2).lastSubmitted()).isNull();
        assertThat(rows.get(2).pcbOnly()).isFalse();
    }

    private static Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCustomer(name);
        customer.setState("MA");
        return customer;
    }
}
