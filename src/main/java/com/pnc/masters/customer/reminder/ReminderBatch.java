package com.pnc.masters.customer.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblreminder_batch")
public class ReminderBatch {

    public static final String NO_LEAD = "no-lead";
    public static final String ANNOUNCEMENT = "announcement";

    public static final String RUNNING = "running";
    public static final String PAUSED = "paused";
    public static final String STOPPED = "stopped";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "LONGTEXT")
    private String htmlBody;

    @Column(name = "cc_recipients", length = 3000)
    private String ccRecipients;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "started_by_user_id", nullable = false)
    private Long startedByUserId;

    @Column(name = "started_by_name", nullable = false, length = 200)
    private String startedByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getHtmlBody() { return htmlBody; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
    public String getCcRecipients() { return ccRecipients; }
    public void setCcRecipients(String ccRecipients) { this.ccRecipients = ccRecipients; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getStartedByUserId() { return startedByUserId; }
    public void setStartedByUserId(Long startedByUserId) { this.startedByUserId = startedByUserId; }
    public String getStartedByName() { return startedByName; }
    public void setStartedByName(String startedByName) { this.startedByName = startedByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
