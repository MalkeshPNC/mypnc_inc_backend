package com.pnc.masters.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSalesPersonRepository extends JpaRepository<CustomerSalesPerson, Long> {

    // Deliberately unfiltered by Customer.isDeleted: assignments of soft-deleted
    // customers still hold the foreign key that blocks a salesperson delete.
    boolean existsBySalesPersonSpId(Long spId);
}
