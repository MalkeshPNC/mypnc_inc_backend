package com.pnc.masters.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	List<Customer> findAllByIsDeletedFalse();

	Optional<Customer> findByCustIdAndIsDeletedFalse(Long custId);

	@Query("""
			SELECT c FROM Customer c
			WHERE c.isDeleted = false
			  AND c.custEntryDt >= :start
			  AND c.custEntryDt < :end
			""")
	List<Customer> findAddedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
