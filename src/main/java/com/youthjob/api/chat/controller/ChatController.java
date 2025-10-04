package com.youthjob.api.chat.controller;

import com.youthjob.api.chat.dto.ChatMessageRequest;
import com.youthjob.api.chat.dto.ChatMessageResponse;
import com.youthjob.api.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "OPENAI API 기반 챗봇 채팅 요청", description = "사용자의 메시지를 입력받아 gpt-4o-mini 모델의 응답을 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="항목 없음")
    })
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
