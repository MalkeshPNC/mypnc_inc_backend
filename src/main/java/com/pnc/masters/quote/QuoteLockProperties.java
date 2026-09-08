package com.pnc.masters.quote;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "quote.lock")
public class QuoteLockProperties {

    /** How long a quote edit lock survives without a heartbeat. */
    private long idleTimeoutMs = 300_000L;

    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }
}
