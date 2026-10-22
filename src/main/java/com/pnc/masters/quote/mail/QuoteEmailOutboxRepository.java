package com.pnc.masters.quote.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuoteEmailOutboxRepository extends JpaRepository<QuoteEmailOutbox, Long> {

    Optional<QuoteEmailOutbox> findFirstByStatusOrderByIdAsc(String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update QuoteEmailOutbox row set row.status = 'sending' where row.id = :id and row.status = 'queued'")
    int claim(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update QuoteEmailOutbox row set row.status = 'queued' where row.status = 'sending'")
    int releaseStuck();
}
