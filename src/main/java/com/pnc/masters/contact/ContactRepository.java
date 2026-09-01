package com.pnc.masters.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByCustomerCustomerContainingIgnoreCase(String companyName);

    List<Contact> findByCustomerCustId(Long custId);
}
