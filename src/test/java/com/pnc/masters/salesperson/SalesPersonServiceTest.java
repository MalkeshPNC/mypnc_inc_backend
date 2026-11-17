package com.pnc.masters.salesperson;

import com.pnc.masters.customer.CustomerSalesPersonRepository;
import com.pnc.masters.salesperson.api.SalesPersonInUseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPersonServiceTest {

    @Mock
    private SalesPersonRepository salesPersonRepository;

    @Mock
    private CustomerSalesPersonRepository customerSalesPersonRepository;

    @InjectMocks
    private SalesPersonService salesPersonService;

    @Test
    void deleteRemovesSalesPersonWithoutAssignments() {
        SalesPerson salesPerson = salesPerson(7L);
        when(salesPersonRepository.findById(7L)).thenReturn(Optional.of(salesPerson));
        when(customerSalesPersonRepository.existsBySalesPersonSpId(7L)).thenReturn(false);

        salesPersonService.delete(7L);

        verify(salesPersonRepository).delete(salesPerson);
    }

    @Test
    void deleteRejectsSalesPersonAssignedToCustomers() {
        SalesPerson salesPerson = salesPerson(7L);
        when(salesPersonRepository.findById(7L)).thenReturn(Optional.of(salesPerson));
        when(customerSalesPersonRepository.existsBySalesPersonSpId(7L)).thenReturn(true);

        assertThatThrownBy(() -> salesPersonService.delete(7L))
                .isInstanceOf(SalesPersonInUseException.class)
                .hasMessageContaining("assigned to one or more customers");

        verify(salesPersonRepository, never()).delete(salesPerson);
    }

    private SalesPerson salesPerson(Long id) {
        SalesPerson salesPerson = new SalesPerson();
        salesPerson.setSpId(id);
        salesPerson.setSalesPerson("Sales Person");
        return salesPerson;
    }
}
