package com.pnc.masters.customer.reminder;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderAudienceResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderContactResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderCustomerResponse;
import com.pnc.masters.quote.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReminderAudienceService implements ReminderAudienceLookup {

    private final CustomerRepository customers;
    private final ContactRepository contacts;
    private final QuoteRepository quotes;

    public ReminderAudienceService(CustomerRepository customers,
                                   ContactRepository contacts,
                                   QuoteRepository quotes) {
        this.customers = customers;
        this.contacts = contacts;
        this.quotes = quotes;
    }

    @Transactional(readOnly = true)
    public ReminderAudienceResponse audience(String kind) {
        requireKind(kind);
        return new ReminderAudienceResponse(kind, rows(kind));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> customerIds(String kind) {
        requireKind(kind);
        return rows(kind).stream().map(ReminderCustomerResponse::custId).collect(Collectors.toSet());
    }

    static void requireKind(String kind) {
        if (!ReminderBatch.NO_LEAD.equals(kind) && !ReminderBatch.ANNOUNCEMENT.equals(kind)) {
            throw new ReminderValidationException("Choose a reminder or an announcement.");
        }
    }

    /** True when the quote type contains "pcb only", including a prefixed label such as "R - PCB only". */
    static boolean pcbOnly(String quoteType) {
        if (quoteType == null) {
            return false;
        }
        String type = quoteType.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return type.contains("pcb only");
    }

    private List<ReminderCustomerResponse> rows(String kind) {
        List<Customer> all = customers.findAllByIsDeletedFalseOrderByCustEntryDtDescCustIdDesc();
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        Map<Long, SubmittedQuote> latest = latestSubmitted();
        Set<Long> ordered = orderedCustomers();
        List<Long> ids = new ArrayList<>();
        for (Customer customer : all) {
            if (include(kind, customer.getCustId(), latest.containsKey(customer.getCustId()), ordered)) {
                ids.add(customer.getCustId());
            }
        }
        Map<Long, List<ReminderContactResponse>> people = contactsFor(ids);
        List<ReminderCustomerResponse> rows = new ArrayList<>();
        for (Customer customer : all) {
            if (!ids.contains(customer.getCustId())) {
                continue;
            }
            SubmittedQuote quote = latest.get(customer.getCustId());
            rows.add(new ReminderCustomerResponse(
                    customer.getCustId(),
                    customer.getCustomer(),
                    customer.getFollowupCount(),
                    customer.getLastReminder(),
                    customer.getFollowupBy(),
                    customer.getState(),
                    quote == null ? null : quote.submitDate(),
                    quote != null && pcbOnly(quote.quoteType()),
                    customer.getAnnouncementFollowupCount(),
                    customer.getLastAnnouncement(),
                    people.getOrDefault(customer.getCustId(), List.of())
            ));
        }
        return rows;
    }

    /**
     * No-lead customers have at least one submitted quote and no order.
     * An order is a received date or a quantity line marked received.
     * Announcements include every non-deleted customer.
     */
    static boolean include(String kind, Long custId, boolean submitted, Set<Long> ordered) {
        if (ReminderBatch.ANNOUNCEMENT.equals(kind)) {
            return true;
        }
        return submitted && !ordered.contains(custId);
    }

    private Map<Long, SubmittedQuote> latestSubmitted() {
        List<Object[]> facts = quotes.findSubmittedFacts();
        if (facts == null || facts.isEmpty()) {
            return Map.of();
        }
        List<SubmittedQuote> parsed = new ArrayList<>();
        for (Object[] row : facts) {
            if (row == null || row.length < 4 || !(row[0] instanceof Number id) || !(row[1] instanceof Number qid)) {
                continue;
            }
            parsed.add(new SubmittedQuote(id.longValue(), qid.longValue(), toDate(row[2]), row[3] == null ? null : row[3].toString()));
        }
        parsed.sort(Comparator
                .comparing(SubmittedQuote::custId)
                .thenComparing(SubmittedQuote::submitDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SubmittedQuote::qid, Comparator.reverseOrder()));
        Map<Long, SubmittedQuote> latest = new LinkedHashMap<>();
        for (SubmittedQuote quote : parsed) {
            latest.putIfAbsent(quote.custId(), quote);
        }
        return latest;
    }

    private Set<Long> orderedCustomers() {
        Set<Long> ordered = new HashSet<>();
        addIds(ordered, quotes.findCustomerIdsWithReceivedDate());
        addIds(ordered, quotes.findCustomerIdsWithReceivedLine());
        return ordered;
    }

    private static void addIds(Set<Long> target, List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                target.add(id);
            }
        }
    }

    private Map<Long, List<ReminderContactResponse>> contactsFor(List<Long> custIds) {
        if (custIds.isEmpty()) {
            return Map.of();
        }
        List<Contact> found = contacts.findWithCustomerByCustIdIn(custIds);
        Map<Long, List<ReminderContactResponse>> grouped = new HashMap<>();
        if (found == null) {
            return grouped;
        }
        for (Contact contact : found) {
            if (contact.getCustomer() == null || contact.getCustomer().getCustId() == null) {
                continue;
            }
            grouped.computeIfAbsent(contact.getCustomer().getCustId(), id -> new ArrayList<>())
                    .add(new ReminderContactResponse(contact.getContId(), contactLabel(contact), blankToEmpty(contact.getEmail())));
        }
        return grouped;
    }

    private static String contactLabel(Contact contact) {
        String name = Stream.of(contact.getFirstName(), contact.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        if (!name.isBlank()) {
            return name;
        }
        if (contact.getContactPerson() != null && !contact.getContactPerson().isBlank()) {
            return contact.getContactPerson().trim();
        }
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
            return contact.getEmail().trim();
        }
        return "Contact #" + contact.getContId();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return null;
    }

    private record SubmittedQuote(long custId, long qid, LocalDate submitDate, String quoteType) {
    }
}
