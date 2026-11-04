package com.pnc.masters.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskReviewerRepository extends JpaRepository<TaskReviewer, TaskReviewer.Key> {

    List<TaskReviewer> findByTaskIdIn(Collection<Long> taskIds);

    List<TaskReviewer> findByTaskIdOrderByUserIdAsc(Long taskId);

    Optional<TaskReviewer> findByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskId(Long taskId);

    void deleteByTaskIdAndUserIdNotIn(Long taskId, Collection<Long> userIds);
}
