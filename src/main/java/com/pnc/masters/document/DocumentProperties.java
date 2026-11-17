package com.pnc.masters.document;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {

    private long maxFileSize = 10 * 1024 * 1024;
    private Set<String> allowedContentTypes = Set.of();

    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
    public Set<String> getAllowedContentTypes() { return allowedContentTypes; }
    public void setAllowedContentTypes(Set<String> allowedContentTypes) { this.allowedContentTypes = allowedContentTypes; }
}