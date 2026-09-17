package com.carefinder.backend.chat;

import com.carefinder.backend.common.ApiMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "CareFinder Assistant")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/v1/chatbot/messages")
    ChatDtos.ChatResponse reply(@Valid @RequestBody ChatDtos.ChatRequest request) {
        return chatService.reply(request);
    }

    @GetMapping("/api/v1/me/chat-history")
    List<ChatDtos.ChatHistoryResponse> history() {
        return chatService.history();
    }

    @DeleteMapping("/api/v1/me/chat-history")
    ApiMessage clearHistory() {
        chatService.clearHistory();
        return new ApiMessage("Chat history cleared.");
    }
}
