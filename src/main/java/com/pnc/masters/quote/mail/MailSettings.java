package com.pnc.masters.quote.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public class MailSettings {

    private String fromAddress = "";
    private String fromName = "PNC Inc";
    private String smtpHost = "";
    private int smtpPort = 587;
    private String smtpUsername = "";
    private String smtpPassword = "";
    private boolean smtpStarttls = true;

    public boolean isConfigured() {
        return notBlank(smtpHost) && notBlank(fromAddress);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
    public boolean isSmtpStarttls() { return smtpStarttls; }
    public void setSmtpStarttls(boolean smtpStarttls) { this.smtpStarttls = smtpStarttls; }
}
