package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuoteContactId implements Serializable {

    private Long quoteId;

    @Column(name = "cont_id", nullable = false)
    private Long contId;

    public QuoteContactId() {
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public Long getContId() {
        return contId;
    }

    public void setContId(Long contId) {
        this.contId = contId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuoteContactId that)) {
            return false;
        }
        return Objects.equals(quoteId, that.quoteId) && Objects.equals(contId, that.contId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quoteId, contId);
    }
}
