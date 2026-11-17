package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatConversationResponse;
import com.pnc.masters.chat.api.ChatCreateConversationRequest;
import com.pnc.masters.chat.api.ChatMessageResponse;
import com.pnc.masters.chat.api.ChatPersonResponse;
import com.pnc.masters.chat.api.ChatSendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/people")
    public List<ChatPersonResponse> people(Authentication authentication) {
        return chatService.listPeople(userId(authentication));
    }

    @GetMapping("/conversations")
    public List<ChatConversationResponse> conversations(Authentication authentication) {
        return chatService.listConversations(userId(authentication));
    }

    @PostMapping("/conversations")
    public ChatConversationResponse open(
            @Valid @RequestBody ChatCreateConversationRequest request,
            Authentication authentication
    ) {
        return chatService.openConversation(userId(authentication), request);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatMessageResponse> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        return chatService.listMessages(userId(authentication), id, beforeId, limit);
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatMessageResponse send(
            @PathVariable Long id,
            @Valid @RequestBody ChatSendMessageRequest request,
            Authentication authentication
    ) {
        return chatService.send(userId(authentication), id, request);
    }

    @PutMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Authentication authentication) {
        chatService.markRead(userId(authentication), id);
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
