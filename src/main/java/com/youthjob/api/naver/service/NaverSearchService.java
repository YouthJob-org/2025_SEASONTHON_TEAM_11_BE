// src/main/java/com/youthjob/api/naver/service/NaverSearchService.java
package com.youthjob.api.naver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class NaverSearchService {

    @Value("${naver.search.client-id}")
    private String clientId;

    @Value("${naver.search.client-secret}")
    private String clientSecret;

    public String searchBlogs(String query, int display, int start, String sort) {
        try {
            final String q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());

            final String apiUrl =
                    "https://openapi.naver.com/v1/search/blog.json"
                            + "?query="  + q
                            + "&display=" + display
                            + "&start="   + start
                            + "&sort="    + sort; // sim | date

            HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(7000);
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

            int status = con.getResponseCode();
            InputStream body = (status == HttpURLConnection.HTTP_OK)
                    ? con.getInputStream()
                    : con.getErrorStream();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString(); // 네이버의 JSON 문자열 그대로 반환
            } finally {
                con.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException("Naver blog search request failed", e);
        }
    }
}
