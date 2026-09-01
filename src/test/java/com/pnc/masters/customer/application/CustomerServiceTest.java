package com.pnc.masters.customer.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import com.pnc.masters.customer.api.CustomerRequest;
import com.pnc.masters.customer.api.CustomerSalesPersonRequest;
import com.pnc.masters.customer.api.CustomerResponse;
import com.pnc.masters.salesperson.SalesPerson;
import com.pnc.masters.salesperson.SalesPersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SalesPersonRepository salesPersonRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createMapsRequestAndPersistsCustomer() {
        CustomerRequest request = request("Acme Inc");
        Customer saved = new Customer();
        saved.setCustId(1L);
        saved.setCustomer("Acme Inc");
        SalesPerson salesPerson = new SalesPerson();
        salesPerson.setSpId(5L);
        salesPerson.setSalesPerson("Sales Person");
        when(salesPersonRepository.findById(5L)).thenReturn(Optional.of(salesPerson));
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse response = customerService.create(request);

        assertThat(response.custId()).isEqualTo(1L);
        assertThat(response.customer()).isEqualTo("Acme Inc");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void findAllReturnsOnlyActiveCustomers() {
        Customer active = new Customer();
        active.setCustId(1L);
        active.setCustomer("Active customer");
        when(customerRepository.findAllByIsDeletedFalse()).thenReturn(List.of(active));

        List<CustomerResponse> response = customerService.findAll();

        assertThat(response).extracting(CustomerResponse::customer).containsExactly("Active customer");
        verify(customerRepository).findAllByIsDeletedFalse();
        verify(customerRepository, never()).findAll();
    }

    @Test
    void findByIdThrowsWhenCustomerDoesNotExist() {
        when(customerRepository.findByCustIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer with id 99 was not found");
    }

    @Test
    void deleteMarksCustomerDeletedInsteadOfPhysicallyDeletingIt() {
        Customer customer = new Customer();
        customer.setCustId(1L);
        customer.setCustomer("Acme Inc");
        when(customerRepository.findByCustIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        assertThat(customer.isDeleted()).isTrue();
        verify(customerRepository).save(customer);
        verify(customerRepository, never()).delete(any(Customer.class));
    }

            @Test
            void duplicateSalespersonAssignmentsArePersistedOnlyOnce() {
            CustomerRequest request = new CustomerRequest(
                "Acme Inc", null,
                List.of(
                    new CustomerSalesPersonRequest(5L, new BigDecimal("12.50")),
                    new CustomerSalesPersonRequest(5L, new BigDecimal("15.00"))
                ),
                null, null, null, null, null, null, null, null, null, false, null
            );
            SalesPerson salesPerson = new SalesPerson();
            salesPerson.setSpId(5L);
            salesPerson.setSalesPerson("Sales Person");
            when(salesPersonRepository.findById(5L)).thenReturn(Optional.of(salesPerson));
            Customer saved = new Customer();
            saved.setCustId(1L);
            saved.setCustomer("Acme Inc");
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            customerService.create(request);

            org.mockito.ArgumentCaptor<Customer> captor = org.mockito.ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getSalesPersons()).hasSize(1);
            assertThat(captor.getValue().getSalesPersons().get(0).getCommission()).isEqualByComparingTo("15.00");
            verify(salesPersonRepository).findById(eq(5L));
            }

    private CustomerRequest request(String customer) {
        return new CustomerRequest(
                customer,
                null,
                List.of(new CustomerSalesPersonRequest(5L, new BigDecimal("12.50"))),
                null,
                null,
                null,
                "1 Main Street",
                "Boston",
                "MA",
                "02108",
                null,
                null,
                true,
                new BigDecimal("12.50")
        );
    }
}
