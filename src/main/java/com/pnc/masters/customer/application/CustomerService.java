package com.pnc.masters.customer.application;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerSalesPerson;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerSalesPersonResponse;
import com.pnc.masters.customer.api.CustomerRequest;
import com.pnc.masters.customer.api.CustomerResponse;
import com.pnc.masters.customer.api.CustomerSalesPersonRequest;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import com.pnc.masters.quote.application.CustomerWinRatio;
import com.pnc.masters.salesperson.SalesPerson;
import com.pnc.masters.salesperson.SalesPersonRepository;
import com.pnc.masters.salesperson.api.SalesPersonNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesPersonRepository salesPersonRepository;
    private final ContactRepository contactRepository;
    private final CustomerWinRatio customerWinRatio;

    public CustomerService(CustomerRepository customerRepository,
                           SalesPersonRepository salesPersonRepository,
                           ContactRepository contactRepository,
                           CustomerWinRatio customerWinRatio) {
        this.customerRepository = customerRepository;
        this.salesPersonRepository = salesPersonRepository;
        this.contactRepository = contactRepository;
        this.customerWinRatio = customerWinRatio;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        List<Customer> customers = customerRepository.findAllByIsDeletedFalseOrderByCustEntryDtDescCustIdDesc();
        Map<Long, String> firstContacts = firstContactNames(customers.stream().map(Customer::getCustId).toList());
        return customers.stream()
                .map(customer -> toResponse(customer, firstContacts.get(customer.getCustId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return findById(id, false);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id, boolean includeWinRatio) {
        Customer customer = getCustomer(id);
        return toResponse(customer, firstContactName(customer.getCustId()), true, includeWinRatio);
    }

    /**
     * One active customer for an open quote that has a name and no cust id.
     * Several customers with that name are left unresolved.
     */
    @Transactional(readOnly = true)
    public CustomerResponse findByName(String name) {
        return findByName(name, false);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findByName(String name, boolean includeWinRatio) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new CustomerNotFoundException(normalized);
        }
        List<Customer> matches = customerRepository.findActiveByName(normalized);
        if (matches.size() != 1) {
            throw new CustomerNotFoundException(normalized);
        }
        Customer customer = matches.get(0);
        return toResponse(customer, firstContactName(customer.getCustId()), true, includeWinRatio);
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        applyRequest(customer, request);
        Customer saved = customerRepository.save(customer);
        return toResponse(saved, firstContactName(saved.getCustId()), true);
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        applyRequest(customer, request);
        Customer saved = customerRepository.save(customer);
        return toResponse(saved, firstContactName(saved.getCustId()), true);
    }

    public void delete(Long id) {
        Customer customer = getCustomer(id);
        customer.setDeleted(true);
        customerRepository.save(customer);
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findByCustIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setCustomer(request.customer());
        customer.setCompanyLogo(request.companyLogo());
        customer.setSalesPersonDefaultCommission(request.salesPersonDefaultCommission());
        applySalesPersonAssignments(customer, request.salesPersons());
        // cust_entry_dt is NOT NULL and defaulted on insert, so an update must not clear it.
        if (request.custEntryDt() != null) {
            customer.setCustEntryDt(request.custEntryDt());
        }
        customer.setReferredBy(request.referredBy());
        customer.setRemarks(request.remarks());
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setZip(request.zip());
        customer.setBilltoAddress(request.billtoAddress());
        customer.setShiptoAddress(request.shiptoAddress());
        customer.setAutomailOn(Boolean.TRUE.equals(request.automailOn()));
    }

    private void applySalesPersonAssignments(Customer customer,
                                             List<CustomerSalesPersonRequest> assignments) {
        Map<Long, CustomerSalesPersonRequest> requestedAssignments = new LinkedHashMap<>();
        if (assignments != null) {
            assignments.forEach(assignment ->
                    requestedAssignments.put(assignment.salesPersonId(), assignment));
        }

        customer.getSalesPersons().removeIf(existing ->
                !requestedAssignments.containsKey(existing.getSalesPerson().getSpId()));

        requestedAssignments.forEach((salesPersonId, assignment) -> {
            CustomerSalesPerson existing = customer.getSalesPersons().stream()
                    .filter(item -> item.getSalesPerson().getSpId().equals(salesPersonId))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                SalesPerson salesPerson = salesPersonRepository.findById(salesPersonId)
                        .orElseThrow(() -> new SalesPersonNotFoundException(salesPersonId));
                existing = new CustomerSalesPerson();
                existing.setCustomer(customer);
                existing.setSalesPerson(salesPerson);
                customer.getSalesPersons().add(existing);
            }

            existing.setCommission(assignment.commission());
        });
    }

    /**
     * The first contact in list order (lowest id). Later contacts stay on the
     * contact screen; this name is the one shown beside the customer.
     */
    private Map<Long, String> firstContactNames(List<Long> custIds) {
        List<Long> ids = custIds.stream().filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Contact> contacts = contactRepository.findByCustomerCustIdInOrderByContIdAsc(ids);
        if (contacts == null) {
            return Map.of();
        }
        Map<Long, String> first = new LinkedHashMap<>();
        for (Contact contact : contacts) {
            Long custId = contact.getCustomer().getCustId();
            first.putIfAbsent(custId, contactLabel(contact));
        }
        return first;
    }

    private String firstContactName(Long custId) {
        if (custId == null) {
            return null;
        }
        List<Contact> contacts = contactRepository.findByCustomerCustIdOrderByContIdAsc(custId);
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }
        return contactLabel(contacts.get(0));
    }

    private static String contactLabel(Contact contact) {
        String name = Stream.of(contact.getFirstName(), contact.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        if (!name.isBlank()) {
            return name;
        }
        if (notBlank(contact.getContactPerson())) {
            return contact.getContactPerson().trim();
        }
        if (notBlank(contact.getEmail())) {
            return contact.getEmail().trim();
        }
        if (notBlank(contact.getPhone())) {
            return contact.getPhone().trim();
        }
        return "Contact #" + contact.getContId();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private CustomerResponse toResponse(Customer customer, String contactPerson) {
        return toResponse(customer, contactPerson, false);
    }

    private CustomerResponse toResponse(Customer customer, String contactPerson, boolean includeBlobLogo) {
        return toResponse(customer, contactPerson, includeBlobLogo, false);
    }

    private CustomerResponse toResponse(Customer customer, String contactPerson, boolean includeBlobLogo, boolean includeWinRatio) {
        return new CustomerResponse(
                customer.getCustId(),
                customer.getCustomer(),
                customer.getCompanyLogo(),
                customer.getSalesPersons().stream().map(assignment -> new CustomerSalesPersonResponse(
                    assignment.getSalesPerson().getSpId(),
                    assignment.getSalesPerson().getSalesPerson(),
                    assignment.getSalesPerson().getSpEmail(),
                    assignment.getCommission()
                )).toList(),
                customer.getCustEntryDt(),
                customer.getReferredBy(),
                customer.getRemarks(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getZip(),
                customer.getBilltoAddress(),
                customer.getShiptoAddress(),
                customer.isAutomailOn(),
                customer.getSalesPersonDefaultCommission(),
                contactPerson,
                includeBlobLogo ? blobPreview(customer) : null,
                includeWinRatio ? customerWinRatio.currentYear(customer.getCustId(), customer.getCustomer()) : null
        );
    }

    /** Data URL for preview and the quote PDF. Omitted when a saved logo file already exists. */
    private String blobPreview(Customer customer) {
        if (notBlank(customer.getCompanyLogo())) {
            return null;
        }
        return CompanyLogoBlob.decode(customerRepository.findCompanyBlobLogo(customer.getCustId()))
                .map(image -> "data:" + image.mediaType() + ";base64,"
                        + Base64.getEncoder().encodeToString(image.bytes()))
                .orElse(null);
    }
}
