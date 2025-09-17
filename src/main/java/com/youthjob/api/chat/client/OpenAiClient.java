package com.youthjob.api.chat.client;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class OpenAiClient {

    private final OpenAiService service;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;

    public OpenAiClient(
            @Value("${chatbot.api-key}") String apiKey,
            @Value("${chatbot.model:gpt-4o-mini}") String model,
            @Value("${chatbot.max-tokens:512}") Integer maxTokens,
            @Value("${chatbot.temperature:0.4}") Double temperature
    ) {
        this.service = new OpenAiService(apiKey, Duration.ofSeconds(30));
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    public String ask(String systemPrompt, String userPrompt) {
        ChatMessage system = new ChatMessage("system", systemPrompt);
        ChatMessage user   = new ChatMessage("user",   userPrompt);

        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(system, user))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        return service.createChatCompletion(req)
                .getChoices().get(0)
                .getMessage().getContent();
    }
}
