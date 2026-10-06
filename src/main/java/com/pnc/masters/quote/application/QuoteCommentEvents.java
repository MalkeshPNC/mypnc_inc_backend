package com.pnc.masters.quote.application;

import com.pnc.masters.quote.api.QuoteCommentResponse;

/** Tells open comment drawers that a quote just received a comment. */
public interface QuoteCommentEvents {

    void published(Long qid, QuoteCommentResponse comment);
}
