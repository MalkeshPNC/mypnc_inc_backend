package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteCommentRepository extends JpaRepository<QuoteComment, Long> {

    List<QuoteComment> findByQidOrderByCreatedAtAsc(Long qid);
}
