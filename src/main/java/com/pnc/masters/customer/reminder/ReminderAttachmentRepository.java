package com.pnc.masters.customer.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderAttachmentRepository extends JpaRepository<ReminderAttachment, Long> {

    List<ReminderAttachment> findByBatchIdOrderByIdAsc(Long batchId);
}
