package com.pnc.masters.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByUpdatedAtDescTaskIdDesc();

    List<Task> findByQidOrderByUpdatedAtDescTaskIdDesc(Long qid);

    @Query("""
            select task from Task task
            where task.reminderDate is not null
              and task.reminderDate <= :today
              and task.status <> 'DONE'
              and (task.reminderNotifiedOn is null or task.reminderNotifiedOn <> task.reminderDate)
            """)
    List<Task> findRemindersDue(@Param("today") LocalDate today);
}
