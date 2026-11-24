package com.pnc.masters.customer.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderBatchRepository extends JpaRepository<ReminderBatch, Long> {

    List<ReminderBatch> findByKindOrderByIdDesc(String kind);
}
