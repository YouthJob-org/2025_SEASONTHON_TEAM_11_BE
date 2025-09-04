package com.youthjob.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    @NotBlank(message = "message는 비어 있을 수 없습니다.")
    private String message;
}
