package com.pnc.masters.salesperson;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesPersonRepository extends JpaRepository<SalesPerson, Long> {
}