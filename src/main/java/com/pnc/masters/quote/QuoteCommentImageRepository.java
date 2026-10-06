package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteCommentImageRepository extends JpaRepository<QuoteCommentImage, Long> {

    List<QuoteCommentImage> findByCommentIdIn(Collection<Long> commentIds);

    Optional<QuoteCommentImage> findByImageIdAndQid(Long imageId, Long qid);
}
