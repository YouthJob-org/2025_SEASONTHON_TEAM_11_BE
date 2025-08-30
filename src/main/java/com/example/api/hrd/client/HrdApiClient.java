package com.example.api.hrd.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class HrdApiClient {

    private final WebClient webClient;

    @Value("${hrd.api.base-url}")
    private String baseUrl;
    @Value("${hrd.api.auth-key}")
    private String authKey;

    public String search(String startDt, String endDt, int page, int size, String area1, String ncs1, String sort, String sortCol) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl.replace("https://www.work24.go.kr", "")) // WebClient 기본 도메인 미사용 시 그대로 path로
                        .scheme("https")
                        .host("www.work24.go.kr")
                        .queryParam("authKey", authKey)
                        .queryParam("returnType", "JSON")
                        .queryParam("outType", "1")
                        .queryParam("pageNum", page)
                        .queryParam("pageSize", size)
                        .queryParam("srchTraStDt", startDt)
                        .queryParam("srchTraEndDt", endDt)
                        .queryParamIfPresent("srchTraArea1", area1 == null || area1.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(area1))
                        .queryParamIfPresent("srchNcs1", ncs1 == null || ncs1.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(ncs1))
                        .queryParam("sort", sort == null ? "ASC" : sort)
                        .queryParam("sortCol", sortCol == null ? "2" : sortCol)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
