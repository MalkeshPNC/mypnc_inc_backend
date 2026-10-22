package com.pnc.masters.quote.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class CompanyMailbox implements QuoteMailbox {

    private final MailSettings settings;

    public CompanyMailbox(MailSettings settings) {
        this.settings = settings;
    }

    @Override
    public void send(QuoteEmailOutbox message) {
        if (!settings.isConfigured()) {
            throw new IllegalStateException("Mail is not configured.");
        }
        if (message.getPdfContent() == null || message.getPdfContent().length == 0) {
            throw new IllegalStateException("The quote PDF is missing.");
        }
        JavaMailSenderImpl mail = sender();
        try {
            MimeMessage mime = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            String fromName = settings.getFromName() == null || settings.getFromName().isBlank()
                    ? settings.getFromAddress()
                    : settings.getFromName().trim();
            helper.setFrom(new InternetAddress(settings.getFromAddress().trim(), fromName, StandardCharsets.UTF_8.name()));
            helper.setReplyTo(settings.getFromAddress().trim());
            helper.setTo(split(message.getRecipients()));
            String[] cc = split(message.getCcRecipients());
            if (cc.length > 0) {
                helper.setCc(cc);
            }
            helper.setSubject(message.getSubject());
            helper.setText(message.getHtmlBody(), true);
            helper.addAttachment(
                    message.getAttachmentName(),
                    new ByteArrayResource(message.getPdfContent()),
                    "application/pdf");
            mail.send(mime);
        } catch (Exception exception) {
            String detail = exception.getMessage();
            throw new IllegalStateException(
                    detail == null || detail.isBlank() ? "The quote email could not be sent." : detail,
                    exception);
        }
    }

    private JavaMailSenderImpl sender() {
        JavaMailSenderImpl mail = new JavaMailSenderImpl();
        mail.setHost(settings.getSmtpHost().trim());
        mail.setPort(settings.getSmtpPort());
        if (settings.getSmtpUsername() != null && !settings.getSmtpUsername().isBlank()) {
            mail.setUsername(settings.getSmtpUsername());
            mail.setPassword(settings.getSmtpPassword());
        }
        Properties properties = mail.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", Boolean.toString(mail.getUsername() != null));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(settings.isSmtpStarttls()));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "20000");
        properties.put("mail.smtp.writetimeout", "20000");
        return mail;
    }

    private static String[] split(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }
        return raw.split(",\\s*");
    }
}
