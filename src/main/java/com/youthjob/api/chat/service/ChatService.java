package com.youthjob.api.chat.service;

import com.youthjob.api.chat.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiClient openAi;

    // 청년 맞춤 톤 고정 (필요하면 자유롭게 수정)
    private static final String SYSTEM_PROMPT = """
        너는 YouthJob의 챗봇이자, 청년들의 취업·교육·역량개발을 돕는 멘토다.
        - 말투: 따뜻하고 간결, 실용적인 조언 위주.
        - HRD 훈련과정/취업역량(EMP) 프로그램/청년정책 안내를 중심으로 답변해.
        - 가능하면 다음 행동(예: 찜하기, 상세보기 링크, 검색 조건 제안)도 제안해.
        - 3~5문장 내로 핵심만 말해.
        - YouthJob은 당신을 응원한다는 응원의 한마디도 해줘.
        """;

    public String reply(String userMessage) {
        try {
            return openAi.ask(SYSTEM_PROMPT, userMessage);
        } catch (Exception e) {
            // 폴백: LLM 장애 시에도 기본 답변
            return "요청을 처리하는 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.";
        }
    }
}
