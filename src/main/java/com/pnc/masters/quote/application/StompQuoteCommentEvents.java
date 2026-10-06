package com.pnc.masters.quote.application;

import com.pnc.masters.quote.api.QuoteCommentResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompQuoteCommentEvents implements QuoteCommentEvents {

    private final SimpMessagingTemplate broker;

    public StompQuoteCommentEvents(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @Override
    public void published(Long qid, QuoteCommentResponse comment) {
        broker.convertAndSend("/topic/quote-comments/" + qid, comment);
    }
}
