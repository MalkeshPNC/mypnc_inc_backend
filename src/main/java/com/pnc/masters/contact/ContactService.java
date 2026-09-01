package com.pnc.masters.contact;

import com.pnc.masters.contact.api.ContactNotFoundException;
import com.pnc.masters.contact.api.ContactRequest;
import com.pnc.masters.contact.api.ContactResponse;
import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;
    private final CustomerRepository customerRepository;

    public ContactService(ContactRepository contactRepository, CustomerRepository customerRepository) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findAll() {
        return contactRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContactResponse findById(Long id) {
        return toResponse(getContact(id));
    }

    public ContactResponse create(ContactRequest request) {
        Contact contact = new Contact();
        applyRequest(contact, request);
        return toResponse(contactRepository.save(contact));
    }

    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = getContact(id);
        applyRequest(contact, request);
        return toResponse(contactRepository.save(contact));
    }

    public void delete(Long id) {
        contactRepository.delete(getContact(id));
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findByCustomerId(Long customerId) {
        return contactRepository.findByCustomerCustId(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Contact> findByCompanyName(String companyName) {
        return contactRepository.findByCustomerCustomerContainingIgnoreCase(companyName);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findByCompanyNameResponses(String companyName) {
        return findByCompanyName(companyName).stream().map(this::toResponse).toList();
    }

    private Contact getContact(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
    }

    private void applyRequest(Contact contact, ContactRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
        contact.setCustomer(customer);
        contact.setFirstName(request.firstName());
        contact.setLastName(request.lastName());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setContactPerson(request.contactPerson());
    }

    private ContactResponse toResponse(Contact contact) {
        return new ContactResponse(
                contact.getContId(),
                contact.getCustomer().getCustId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getContactPerson()
        );
    }
}
