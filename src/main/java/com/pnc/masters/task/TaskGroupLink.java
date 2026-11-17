package com.pnc.masters.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tbltask_groups")
@IdClass(TaskGroupLink.Key.class)
public class TaskGroupLink {

    @Id
    @Column(name = "task_id")
    private Long taskId;

    @Id
    @Column(name = "group_id")
    private Long groupId;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public static class Key implements Serializable {
        private Long taskId;
        private Long groupId;

        public Key() {
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(taskId, key.taskId) && Objects.equals(groupId, key.groupId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, groupId);
        }
    }
}
