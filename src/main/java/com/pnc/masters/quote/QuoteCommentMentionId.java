package com.pnc.masters.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuoteCommentMentionId implements Serializable {

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "user_id")
    private Long userId;

    public QuoteCommentMentionId() {
    }

    public QuoteCommentMentionId(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    public Long getCommentId() { return commentId; }
    public Long getUserId() { return userId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuoteCommentMentionId that)) {
            return false;
        }
        return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId, userId);
    }
}
