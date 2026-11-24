package com.pnc.masters.customer.reminder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReminderRecipientRepository extends JpaRepository<ReminderRecipient, Long> {

    List<ReminderRecipient> findByBatchIdOrderByIdAsc(Long batchId);

    List<ReminderRecipient> findByStatus(String status);

    List<ReminderRecipient> findByBatchIdIn(Collection<Long> batchIds);

    long countByBatchIdAndCustomerIdAndStatusAndIdNot(Long batchId, Long customerId, String status, Long id);

    /**
     * Oldest pending row on a running batch. Paused and stopped batches are absent,
     * which is the same rule as {@link ReminderRules#claimable}.
     */
    @Query("""
            SELECT r FROM ReminderRecipient r
            WHERE r.status = 'pending'
              AND r.batchId IN (SELECT b.id FROM ReminderBatch b WHERE b.status = 'running')
            ORDER BY r.id ASC
            """)
    List<ReminderRecipient> findPendingOnRunningBatches(Pageable pageable);

    @Query(value = """
            SELECT r, b FROM ReminderRecipient r, ReminderBatch b
            WHERE r.batchId = b.id
              AND (:kind IS NULL OR b.kind = :kind)
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.id DESC
            """,
            countQuery = """
            SELECT count(r) FROM ReminderRecipient r, ReminderBatch b
            WHERE r.batchId = b.id
              AND (:kind IS NULL OR b.kind = :kind)
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<Object[]> searchLog(@Param("kind") String kind, @Param("status") String status, Pageable pageable);

    /**
     * One pending-to-sending update. A second worker gets 0 when the row is no longer pending
     * or the batch is no longer running.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE tblreminder_recipient r
            INNER JOIN tblreminder_batch b ON b.id = r.batch_id
            SET r.status = 'sending'
            WHERE r.id = :id AND r.status = 'pending' AND b.status = 'running'
            """, nativeQuery = true)
    int claim(@Param("id") Long id);
}
