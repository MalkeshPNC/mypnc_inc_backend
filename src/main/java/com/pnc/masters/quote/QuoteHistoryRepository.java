package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteHistoryRepository extends JpaRepository<QuoteHistory, Long> {

    List<QuoteHistory> findAllByQidOrderByChangedAtDesc(Long qid);
}
