package com.pnc.masters.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	List<Customer> findAllByIsDeletedFalse();

	List<Customer> findAllByIsDeletedFalseOrderByCustEntryDtDescCustIdDesc();

	Optional<Customer> findByCustIdAndIsDeletedFalse(Long custId);

	@Query(value = "SELECT company_blob_logo FROM tblcustomers WHERE cust_id = :id", nativeQuery = true)
	byte[] findCompanyBlobLogo(@Param("id") Long id);

	@Query("""
			SELECT c FROM Customer c
			WHERE c.isDeleted = false
			  AND LOWER(TRIM(c.customer)) = LOWER(TRIM(:name))
			ORDER BY c.custId ASC
			""")
	List<Customer> findActiveByName(@Param("name") String name);

	@Query("""
			SELECT c FROM Customer c
			WHERE c.isDeleted = false
			  AND c.custEntryDt >= :start
			  AND c.custEntryDt < :end
			""")
	List<Customer> findAddedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
