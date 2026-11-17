package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tblquote_comment_mentions")
public class QuoteCommentMention {

    @EmbeddedId
    private QuoteCommentMentionId id;

    public QuoteCommentMention() {
    }

    public QuoteCommentMention(Long commentId, Long userId) {
        this.id = new QuoteCommentMentionId(commentId, userId);
    }

    public QuoteCommentMentionId getId() { return id; }
    public void setId(QuoteCommentMentionId id) { this.id = id; }
}
