package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteAttachmentRepository extends JpaRepository<QuoteAttachment, Long> {

    List<QuoteAttachment> findByQidOrderByCreatedAtDescAttachmentIdDesc(Long qid);

    Optional<QuoteAttachment> findByAttachmentIdAndQid(Long attachmentId, Long qid);

    Optional<QuoteAttachment> findByQidAndCategoryAndOriginalFilenameIgnoreCase(Long qid, String category, String originalFilename);

    List<QuoteAttachment> findByQidAndGeneratedTrue(Long qid);

    List<QuoteAttachment> findByQidAndCategoryOrderByAttachmentIdAsc(Long qid, String category);

    boolean existsByQidAndCategory(Long qid, String category);

    @Query("SELECT DISTINCT a.category FROM QuoteAttachment a WHERE a.qid = :qid ORDER BY a.category")
    List<String> findCategories(@Param("qid") Long qid);
}
