package com.youthjob.api.chat.controller;

import com.youthjob.api.chat.dto.ChatMessageRequest;
import com.youthjob.api.chat.dto.ChatMessageResponse;
import com.youthjob.api.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> chat(@RequestBody @Valid ChatMessageRequest req) {
        String answer = chatService.reply(req.getMessage());
        return ResponseEntity.ok(
                ChatMessageResponse.builder()
                        .answer(answer)
                        .model("gpt-4o-mini")
                        .build()
        );
    }
}
