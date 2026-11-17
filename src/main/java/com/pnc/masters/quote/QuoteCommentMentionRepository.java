package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuoteCommentMentionRepository extends JpaRepository<QuoteCommentMention, QuoteCommentMentionId> {

    List<QuoteCommentMention> findByIdCommentIdIn(Collection<Long> commentIds);
}
