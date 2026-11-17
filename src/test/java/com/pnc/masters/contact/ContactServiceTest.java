package com.pnc.masters.contact;

import com.pnc.masters.customer.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void findByCompanyNameReturnsMatchingContacts() {
        Customer customer = new Customer();
        customer.setCustId(10L);
        customer.setCustomer("Acme Inc");

        Contact contact = new Contact();
        contact.setContId(1L);
        contact.setCustomer(customer);
        contact.setFirstName("John");
        contact.setLastName("Doe");
        contact.setPhone("555-1234");
        contact.setEmail("john@acme.com");
        contact.setContactPerson("John Doe");

        when(contactRepository.findByCustomerCustomerContainingIgnoreCase("Acme")).thenReturn(List.of(contact));

        List<Contact> result = contactService.findByCompanyName("Acme");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john@acme.com");
        verify(contactRepository).findByCustomerCustomerContainingIgnoreCase("Acme");
    }
}
