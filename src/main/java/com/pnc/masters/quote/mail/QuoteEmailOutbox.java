package com.pnc.masters.quote.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tblquote_email_outbox")
public class QuoteEmailOutbox {

    public static final String QUEUED = "queued";
    public static final String SENDING = "sending";
    public static final String SENT = "sent";
    public static final String FAILED = "failed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_id", nullable = false)
    private Long quoteId;

    @Column(name = "quote_number", nullable = false, length = 80)
    private String quoteNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "from_address", nullable = false, length = 200)
    private String fromAddress;

    @Column(nullable = false, length = 3000)
    private String recipients;

    @Column(name = "cc_recipients", length = 3000)
    private String ccRecipients;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "LONGTEXT")
    private String htmlBody;

    @Column(name = "attachment_name", nullable = false, length = 200)
    private String attachmentName;

    @Column(name = "pdf_content", columnDefinition = "LONGBLOB")
    private byte[] pdfContent;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Loaded for sending. Not a column; the bytes live in tblquote_email_files. */
    @Transient
    private List<QuoteEmailFile> extraFiles = List.of();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }
    public String getCcRecipients() { return ccRecipients; }
    public void setCcRecipients(String ccRecipients) { this.ccRecipients = ccRecipients; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getHtmlBody() { return htmlBody; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public byte[] getPdfContent() { return pdfContent; }
    public void setPdfContent(byte[] pdfContent) { this.pdfContent = pdfContent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public List<QuoteEmailFile> getExtraFiles() { return extraFiles; }
    public void setExtraFiles(List<QuoteEmailFile> extraFiles) { this.extraFiles = extraFiles == null ? List.of() : extraFiles; }
}
