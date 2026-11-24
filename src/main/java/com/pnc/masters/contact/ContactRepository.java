package com.pnc.masters.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByCustomerCustomerContainingIgnoreCase(String companyName);

    List<Contact> findByCustomerCustIdOrderByContIdAsc(Long custId);

    List<Contact> findByCustomerCustIdInOrderByContIdAsc(Collection<Long> custIds);

    @Query("""
            SELECT c FROM Contact c
            JOIN FETCH c.customer
            WHERE c.customer.custId IN :custIds
            ORDER BY c.contId ASC
            """)
    List<Contact> findWithCustomerByCustIdIn(@Param("custIds") Collection<Long> custIds);
}
