package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

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
     * Quotes that can affect a year: created in it, or received in it.
     * Quantities come along so the report does not query each quote again.
     */
    @Query("""
            SELECT DISTINCT q FROM Quote q
            LEFT JOIN FETCH q.quantities
            WHERE q.isDeleted = false
              AND (
                (q.createDate >= :start AND q.createDate < :end)
                OR (q.receivedDate >= :start AND q.receivedDate < :end)
              )
            """)
    List<Quote> findForReport(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
