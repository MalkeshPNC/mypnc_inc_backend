package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tblquote_contacts")
public class QuoteContact {

    @EmbeddedId
    private QuoteContactId id = new QuoteContactId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("quoteId")
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryContact;

    public QuoteContactId getId() {
        return id;
    }

    public void setId(QuoteContactId id) {
        this.id = id;
    }

    public Long getContId() {
        return id.getContId();
    }

    public void setContId(Long contId) {
        id.setContId(contId);
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
        if (quote != null && quote.getQid() != null) {
            id.setQuoteId(quote.getQid());
        }
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        this.primaryContact = primaryContact;
    }

    @PrePersist
    void fillQuoteId() {
        if (quote != null) {
            id.setQuoteId(quote.getQid());
        }
    }
}
