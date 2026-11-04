package com.pnc.masters.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, TaskAssignee.Key> {

    List<TaskAssignee> findByTaskIdIn(Collection<Long> taskIds);

    List<TaskAssignee> findByTaskIdOrderByUserIdAsc(Long taskId);

    Optional<TaskAssignee> findByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskId(Long taskId);

    void deleteByTaskIdAndUserIdNotIn(Long taskId, Collection<Long> userIds);
}
