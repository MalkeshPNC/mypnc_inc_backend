package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    Optional<Quote> findByQidAndIsDeletedFalse(Long qid);

    boolean existsByQuoteNumberIgnoreCase(String quoteNumber);

    boolean existsByQuoteNumberIgnoreCaseAndQidNot(String quoteNumber, Long qid);
}
