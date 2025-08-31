package com.youthjob.api.hrd.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${hrd.api.detail-url:https://www.work24.go.kr/cm/openApi/call/hr/callOpenApiSvcInfo310L02.do}")
    private String detailUrl;

    public String getDetail(String trprId, String trprDegr, String torgId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(detailUrl.replace("https://www.work24.go.kr", "")) // host 분리
                        .scheme("https")
                        .host("www.work24.go.kr")
                        .queryParam("authKey", authKey)
                        .queryParam("returnType", "JSON")
                        .queryParam("outType", "2") // 상세
                        .queryParam("srchTrprId", trprId)
                        .queryParam("srchTrprDegr", trprDegr)
                        .queryParam("srchTorgId", torgId)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Value("${hrd.api.stats-url:https://www.work24.go.kr/cm/openApi/call/hr/callOpenApiSvcInfo310L03.do}")
    private String statsUrl;

    /** 310L03: XML로 호출 (JSON은 가끔 HTML로 튀어 실패하므로) */
    public String getStatsXml(String trprId, String torgId, String trprDegrOrNull) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(statsUrl.replace("https://www.work24.go.kr",""))
                        .scheme("https")
                        .host("www.work24.go.kr")
                        .queryParam("authKey", authKey)
                        .queryParam("returnType", "XML")
                        .queryParam("outType", "2")
                        .queryParam("srchTrprId", trprId)
                        .queryParam("srchTorgId", torgId)
                        .queryParamIfPresent("srchTrprDegr",
                                (trprDegrOrNull == null || trprDegrOrNull.isBlank())
                                        ? java.util.Optional.empty()
                                        : java.util.Optional.of(trprDegrOrNull))
                        .build())
                .accept(org.springframework.http.MediaType.APPLICATION_XML)
                .header("Accept-Encoding", "identity")
                .header("User-Agent","Mozilla/5.0")
                .header("Referer","https://www.work24.go.kr/")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
