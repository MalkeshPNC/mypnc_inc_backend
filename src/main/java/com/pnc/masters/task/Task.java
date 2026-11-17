package com.pnc.masters.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbltasks")
public class Task {

    public static final String CREATED = "CREATED";
    public static final String IN_PROCESS = "IN_PROCESS";
    public static final String REVIEW = "REVIEW";
    public static final String DONE = "DONE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "task_type", length = 120)
    private String taskType;

    @Column(length = 20)
    private String priority;

    @Column(name = "job_number", length = 80)
    private String jobNumber;

    @Column(name = "nc_number", length = 80)
    private String ncNumber;

    @Column(name = "qid")
    private Long qid;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "reminder_notified_on")
    private LocalDate reminderNotifiedOn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }
    public String getNcNumber() { return ncNumber; }
    public void setNcNumber(String ncNumber) { this.ncNumber = ncNumber; }
    public Long getQid() { return qid; }
    public void setQid(Long qid) { this.qid = qid; }
    public LocalDate getReminderDate() { return reminderDate; }
    public void setReminderDate(LocalDate reminderDate) { this.reminderDate = reminderDate; }
    public LocalDate getReminderNotifiedOn() { return reminderNotifiedOn; }
    public void setReminderNotifiedOn(LocalDate reminderNotifiedOn) { this.reminderNotifiedOn = reminderNotifiedOn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
