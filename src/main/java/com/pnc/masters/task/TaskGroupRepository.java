package com.pnc.masters.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TaskGroupRepository extends JpaRepository<TaskGroupLink, TaskGroupLink.Key> {

    List<TaskGroupLink> findByTaskIdIn(Collection<Long> taskIds);

    List<TaskGroupLink> findByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);
}
