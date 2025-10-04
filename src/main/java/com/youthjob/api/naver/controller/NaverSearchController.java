// src/main/java/com/youthjob/api/naver/controller/NaverSearchController.java
package com.youthjob.api.naver.controller;

import com.youthjob.api.naver.service.NaverSearchService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Blog", description = "네이버 블로그 조회 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/naver")
public class NaverSearchController {

    private final NaverSearchService naverSearchService;

    @Operation(summary = "네이버 블로그", description = "쿼리(q)값을 기준으로 네이버 블로그 검색하여 목록 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping(value = "/blogs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> blogs(
            @RequestParam("q") String query, //검색 키워드
            @RequestParam(value = "display", defaultValue = "5") int display, //기본값 5개
            @RequestParam(value = "start", defaultValue = "1") int start,
            @RequestParam(value = "sort", defaultValue = "sim") String sort
    ) {
        String json = naverSearchService.searchBlogs(query, display, start, sort);

        Object result;
        try {
            result = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("네이버 블로그 응답 파싱 실패", e);
        }

        return ApiResponse.success(
                SuccessStatus.NAVER_BLOG_SEARCH_SUCCESS,
                result
        );
    }
}
