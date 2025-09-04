package com.youthjob.api.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private final String answer;
    private final String model;
}
