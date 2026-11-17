package com.pnc.masters.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {

    Optional<Document> findByDocumentIdAndDeletedAtIsNull(String documentId);

    @Query("""
            SELECT d FROM Document d
            WHERE d.deletedAt IS NULL
              AND ((:customerId IS NULL AND d.customerId IS NULL) OR d.customerId = :customerId)
              AND (:category IS NULL OR LOWER(d.category) = LOWER(:category))
            ORDER BY d.createdAt DESC
            """)
    List<Document> search(@Param("customerId") Long customerId, @Param("category") String category);
}
