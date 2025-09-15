package com.youthjob.api.hrd.controller;

import com.youthjob.api.hrd.dto.*;
import com.youthjob.api.hrd.service.HrdSearchService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.SuccessStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HRD 검색/상세/통계/저장 API
 * - 목록(search): DB(HrdCourseCatalog)에서 조회
 * - 상세(detail), 통계(stats), 풀(full): 서비스가 DB(HrdCourseFull) 우선 조회 → 없으면 API 호출/업서트
 *   (프론트 파라미터/응답 형식은 그대로 유지)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hrd")
public class HrdController {

    private final HrdSearchService searchService;

    /** 목록 조회: DB 카탈로그에서 페이징/정렬/필터 */
    @GetMapping("/courses")
    public ResponseEntity<List<HrdCourseDto>> search(
            @RequestParam String startDt,
            @RequestParam String endDt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String area1,
            @RequestParam(required = false) String ncs1,
            @RequestParam(defaultValue = "ASC") String sort,
            @RequestParam(defaultValue = "2") String sortCol
    ) {
        return ResponseEntity.ok(
                searchService.search(startDt, endDt, page, size, area1, ncs1, sort, sortCol)
        );
    }

    /** 상세(기관/훈련과정 기본정보): DB(HrdCourseFull) 우선 → 없으면 API 호출 후 업서트 */
    @GetMapping("/courses/{trprId}/{trprDegr}")
    public ResponseEntity<HrdCourseDetailDto> detail(
            @PathVariable String trprId,
            @PathVariable String trprDegr,
            @RequestParam String torgId
    ) {
        return ResponseEntity.ok(
                searchService.getDetail(trprId, trprDegr, torgId)
        );
    }

    /** 통계(수료/취업률 등): DB(HrdCourseFull.stats) 우선 → 없으면 API */
    @GetMapping("/courses/{trprId}/stats")
    public ResponseEntity<List<HrdCourseStatDto>> stats(
            @PathVariable String trprId,
            @RequestParam String torgId,
            @RequestParam(required = false) String trprDegr // 없으면 서비스에서 전체/최신 등 정책 처리
    ) {
        return ResponseEntity.ok(
                searchService.getStats(trprId, torgId, trprDegr)
        );
    }

    /** 상세+통계 묶음: DB 우선 → 없으면 API 호출 후 업서트 */
    @GetMapping("/courses/{trprId}/{trprDegr}/full")
    public ResponseEntity<HrdCourseFullDto> full(
            @PathVariable String trprId,
            @PathVariable String trprDegr,
            @RequestParam String torgId
    ) {
        return ResponseEntity.ok(
                searchService.getCourseFull(trprId, trprDegr, torgId)
        );
    }

    /* ===== 저장 목록/추가/삭제/토글 (기존 프론트 응답 래핑 그대로 유지) ===== */

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedCourseDto>>> listSaved() {
        return ApiResponse.success(
                SuccessStatus.HRD_SAVED_LIST_SUCCESS,
                searchService.listSaved()
        );
    }

    @PostMapping("/saved")
    public ResponseEntity<ApiResponse<SavedCourseDto>> save(@RequestBody @Valid SaveCourseRequest req) {
        return ApiResponse.success(
                SuccessStatus.HRD_SAVED_ADD_SUCCESS,
                searchService.saveCourse(req)
        );
    }

    @DeleteMapping("/saved/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        searchService.deleteSaved(id);
        return ApiResponse.successOnly(SuccessStatus.HRD_SAVED_DELETE_SUCCESS);
    }

    @PostMapping("/saved/toggle")
    public ResponseEntity<ApiResponse<SavedCourseDto>> toggle(@RequestBody @Valid SaveCourseRequest req) {
        return ApiResponse.success(
                SuccessStatus.HRD_SAVED_TOGGLE_SUCCESS,
                searchService.toggleSaved(req)
        );
    }
}
