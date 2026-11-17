package com.pnc.masters.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "tbltask_assignees")
@IdClass(TaskAssignee.Key.class)
public class TaskAssignee {

    @Id
    @Column(name = "task_id")
    private Long taskId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "comment_at")
    private LocalDateTime commentAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCommentAt() { return commentAt; }
    public void setCommentAt(LocalDateTime commentAt) { this.commentAt = commentAt; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class Key implements Serializable {
        private Long taskId;
        private Long userId;

        public Key() {
        }

        public Key(Long taskId, Long userId) {
            this.taskId = taskId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(taskId, key.taskId) && Objects.equals(userId, key.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, userId);
        }
    }
}
