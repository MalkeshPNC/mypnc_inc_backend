package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {

    List<Quote> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    Optional<Quote> findByQidAndIsDeletedFalse(Long qid);

    boolean existsByQuoteNumberIgnoreCase(String quoteNumber);

    boolean existsByQuoteNumberIgnoreCaseAndQidNot(String quoteNumber, Long qid);

    /**
     * Quote numbers already used by this NC, in any year. Matches the stored
     * NC and numbers shaped {@code YYYY} + NC + an optional {@code .digits}
     * revision, so a row still counts when the column and the number disagree.
     * {@code ncLike} is the NC with LIKE wildcards escaped. {@code excludeQid}
     * 0 leaves every quote in, since no quote carries that id.
     */
    @Query("""
            SELECT q.quoteNumber FROM Quote q
            WHERE q.isDeleted = false
              AND q.qid <> :excludeQid
              AND (
                LOWER(TRIM(q.ncNumber)) = LOWER(:ncNumber)
                OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT('____', :ncLike)) ESCAPE '\\'
                OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT('____', :ncLike, '.%')) ESCAPE '\\'
              )
            """)
    List<String> findQuoteNumbersForNc(@Param("ncNumber") String ncNumber,
                                       @Param("ncLike") String ncLike,
                                       @Param("excludeQid") Long excludeQid);

    /**
     * Non-deleted quotes that share an NC, oldest first.
     */
    @Query("""
            SELECT q FROM Quote q
            WHERE q.isDeleted = false
              AND LOWER(TRIM(q.ncNumber)) = LOWER(:ncNumber)
            ORDER BY q.createdAt ASC, q.qid ASC
            """)
    List<Quote> findByNcNumberIgnoreCaseAndIsDeletedFalse(@Param("ncNumber") String ncNumber);

    /**
     * Grand Total of quantity rows marked received, one sum per quote.
     * Quotes with no received row are absent; the caller treats that as zero.
     */
    @Query("""
            SELECT q.qid, SUM(qty.total)
            FROM QuoteQuantity qty
            JOIN qty.quote q
            WHERE qty.received = true
              AND q.qid IN :qids
            GROUP BY q.qid
            """)
    List<Object[]> sumReceivedTotals(@Param("qids") Collection<Long> qids);

    /**
     * Quotes that can affect a year: submitted in it, or received in it.
     * Quantities come along so the report does not query each quote again.
     */
    @Query("""
            SELECT DISTINCT q FROM Quote q
            LEFT JOIN FETCH q.quantities
            WHERE q.isDeleted = false
              AND (
                (q.submitDate >= :start AND q.submitDate < :end)
                OR (q.receivedDate >= :start AND q.receivedDate < :end)
              )
            """)
    List<Quote> findForReport(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Quotes submitted in a date range. Quantities come along so a customer
     * summary does not query each quote again.
     */
    @Query("""
            SELECT DISTINCT q FROM Quote q
            LEFT JOIN FETCH q.quantities
            WHERE q.isDeleted = false
              AND q.submitDate >= :start
              AND q.submitDate < :end
            """)
    List<Quote> findSubmittedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Submitted quotes in a date range, without quantity rows. */
    @Query("""
            SELECT q FROM Quote q
            WHERE q.isDeleted = false
              AND q.submitDate >= :start
              AND q.submitDate < :end
            """)
    List<Quote> findSubmittedHeadersBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** How many non-deleted quotes share each NC on the current list page. */
    @Query("""
            SELECT LOWER(TRIM(q.ncNumber)), COUNT(q)
            FROM Quote q
            WHERE q.isDeleted = false
              AND LOWER(TRIM(q.ncNumber)) IN :ncNumbers
            GROUP BY LOWER(TRIM(q.ncNumber))
            """)
    List<Object[]> countByNcNumber(@Param("ncNumbers") Collection<String> ncNumbers);

    /** How many non-deleted quotes are stored against each NC id. */
    @Query("""
            SELECT q.ncId, COUNT(q)
            FROM Quote q
            WHERE q.isDeleted = false
              AND q.ncId IS NOT NULL
            GROUP BY q.ncId
            """)
    List<Object[]> countByNcId();

    @Query("""
            SELECT COUNT(q) FROM Quote q
            WHERE q.isDeleted = false
              AND q.ncId = :ncId
            """)
    long countActiveByNcId(@Param("ncId") Long ncId);

    @Query("""
            SELECT q FROM Quote q
            WHERE q.isDeleted = false
              AND q.ncId = :ncId
            ORDER BY q.createdAt DESC, q.qid DESC
            """)
    List<Quote> findActiveByNcId(@Param("ncId") Long ncId);

    /** How many non-deleted quotes are stored against each customer. */
    @Query("""
            SELECT q.custId, COUNT(q)
            FROM Quote q
            WHERE q.isDeleted = false
              AND q.custId IS NOT NULL
            GROUP BY q.custId
            """)
    List<Object[]> countByCustId();

    @Query("""
            SELECT COUNT(q) FROM Quote q
            WHERE q.isDeleted = false
              AND q.custId = :custId
            """)
    long countActiveByCustId(@Param("custId") Long custId);

    @Query("""
            SELECT q FROM Quote q
            WHERE q.isDeleted = false
              AND q.custId = :custId
            ORDER BY q.createdAt DESC, q.qid DESC
            """)
    List<Quote> findActiveByCustId(@Param("custId") Long custId);

    /**
     * Submitted quotes for one customer, newest submit date first.
     * The first row is the quote we last gave that customer.
     */
    @Query("""
            SELECT q FROM Quote q
            WHERE q.isDeleted = false
              AND q.custId = :custId
              AND q.submitDate IS NOT NULL
            ORDER BY q.submitDate DESC, q.qid DESC
            """)
    List<Quote> findSubmittedByCustId(@Param("custId") Long custId);

    /**
     * Submitted quotes for reminder lists. Newest submit date per customer is the
     * one that decides Last Submitted and PCB Only.
     */
    @Query("""
            SELECT q.custId, q.qid, q.submitDate, q.quoteType
            FROM Quote q
            WHERE q.isDeleted = false
              AND q.custId IS NOT NULL
              AND q.submitDate IS NOT NULL
            """)
    List<Object[]> findSubmittedFacts();

    /** Customers with a non-deleted quote that has a received date. That quote is an order. */
    @Query("""
            SELECT DISTINCT q.custId
            FROM Quote q
            WHERE q.isDeleted = false
              AND q.custId IS NOT NULL
              AND q.receivedDate IS NOT NULL
            """)
    List<Long> findCustomerIdsWithReceivedDate();

    /** Customers with a quantity line marked received. That line is an order. */
    @Query("""
            SELECT DISTINCT q.custId
            FROM Quote q
            JOIN q.quantities qty
            WHERE q.isDeleted = false
              AND q.custId IS NOT NULL
              AND qty.received = true
            """)
    List<Long> findCustomerIdsWithReceivedLine();
}
