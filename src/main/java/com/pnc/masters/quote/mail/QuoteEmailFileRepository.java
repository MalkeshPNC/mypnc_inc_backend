package com.pnc.masters.quote.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteEmailFileRepository extends JpaRepository<QuoteEmailFile, Long> {

    List<QuoteEmailFile> findByOutboxIdOrderByFileIdAsc(Long outboxId);

    @Modifying(clearAutomatically = true)
    @Query("delete from QuoteEmailFile f where f.outboxId = :outboxId")
    void deleteByOutboxId(@Param("outboxId") Long outboxId);
}
