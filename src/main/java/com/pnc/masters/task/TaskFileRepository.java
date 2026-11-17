package com.pnc.masters.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    List<TaskFile> findByTaskIdOrderByFileIdAsc(Long taskId);

    List<TaskFile> findByTaskIdInOrderByFileIdAsc(Collection<Long> taskIds);
}
