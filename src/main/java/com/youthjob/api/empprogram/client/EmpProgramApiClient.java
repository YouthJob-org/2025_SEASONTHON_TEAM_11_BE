// src/main/java/com/youthjob/api/empprogram/client/EmpProgramApiClient.java
package com.youthjob.api.empprogram.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmpProgramApiClient {

    private final WebClient webClient;

    @Value("${api.emp.key}")
    private String authKey;

    @Value("${api.emp.base-url}") // https://www.work24.go.kr/cm/openApi
    private String baseUrl;

    private static final String SEARCH_PATH = "/call/wk/callOpenApiSvcInfo217L01.do";

    public String getProgramsXml(String pgmStdt, String topOrgCd, String orgCd,
                                 Integer startPage, Integer display) {

        final int page = (startPage == null || startPage < 1) ? 1 : Math.min(startPage, 1000);
        final int size = (display   == null || display   < 1) ? 10 : Math.min(display, 100);

        final String path = (baseUrl + SEARCH_PATH)
                .replace("https://www.work24.go.kr", "")
                .replace("http://www.work24.go.kr", "");

        return webClient.get()
                .uri(b -> b.path(path)
                        .scheme("https")
                        .host("www.work24.go.kr")
                        .queryParam("authKey", authKey)
                        .queryParam("returnType", "XML")
                        .queryParam("startPage", page)
                        .queryParam("display", size)
                        .queryParamIfPresent("pgmStdt",  opt(pgmStdt))
                        .queryParamIfPresent("topOrgCd", opt(topOrgCd))
                        .queryParamIfPresent("orgCd",    opt(orgCd))
                        .build())
                .accept(MediaType.APPLICATION_XML)
                .header("Accept-Encoding", "identity")
                .header("User-Agent", "Mozilla/5.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Optional<String> opt(String v) {
        return (v == null || v.isBlank()) ? Optional.empty() : Optional.of(v);
    }
}
