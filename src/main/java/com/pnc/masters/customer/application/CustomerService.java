package com.pnc.masters.customer.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerSalesPerson;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerSalesPersonResponse;
import com.pnc.masters.customer.api.CustomerRequest;
import com.pnc.masters.customer.api.CustomerResponse;
import com.pnc.masters.customer.api.CustomerSalesPersonRequest;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import com.pnc.masters.salesperson.SalesPerson;
import com.pnc.masters.salesperson.SalesPersonRepository;
import com.pnc.masters.salesperson.api.SalesPersonNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesPersonRepository salesPersonRepository;

    public CustomerService(CustomerRepository customerRepository, SalesPersonRepository salesPersonRepository) {
        this.customerRepository = customerRepository;
        this.salesPersonRepository = salesPersonRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAllByIsDeletedFalse().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return toResponse(getCustomer(id));
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
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

    private CustomerResponse toResponse(Customer customer) {
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
                customer.getSalesPersonDefaultCommission()
        );
    }
}
