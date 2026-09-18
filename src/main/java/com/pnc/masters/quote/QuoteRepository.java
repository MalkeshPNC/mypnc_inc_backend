package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    Optional<Quote> findByQidAndIsDeletedFalse(Long qid);

    boolean existsByQuoteNumberIgnoreCase(String quoteNumber);

    boolean existsByQuoteNumberIgnoreCaseAndQidNot(String quoteNumber, Long qid);

    /**
     * Numbers in the copy family: the bare family itself, plus any
     * {@code family.<suffix>} revisions. Deleted quotes are excluded.
     */
    @Query("""
            SELECT q.quoteNumber FROM Quote q
            WHERE q.isDeleted = false
              AND (
                LOWER(q.quoteNumber) = LOWER(:family)
                OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT(:family, '.%'))
              )
            """)
    List<String> findFamilyQuoteNumbers(@Param("family") String family);

    @Query("""
            SELECT q FROM Quote q
            WHERE q.isDeleted = false
              AND (
                LOWER(q.quoteNumber) = LOWER(:family)
                OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT(:family, '.%'))
              )
            """)
    List<Quote> findFamilyQuotes(@Param("family") String family);
}
