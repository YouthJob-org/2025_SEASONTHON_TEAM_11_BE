package com.youthjob.api.hrd.controller;

import com.youthjob.api.hrd.dto.*;
import com.youthjob.api.hrd.service.HrdSearchService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "HRD", description = "내일배움카드 교육 검색, 상세정보, 저장 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hrd")
public class HrdController {

    private final HrdSearchService searchService;

    @Operation(
            summary = "내일배움 카드 교육 검색",
            description = "지역, 훈련 분야를 기준으로 검색 결과 반환"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<HrdSearchService.SliceResponse<HrdCourseDto>>> search(
            @RequestParam String startDt,
            @RequestParam String endDt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String area1,
            @RequestParam(required = false) String ncs1,
            @RequestParam(defaultValue = "ASC") String sort,
            @RequestParam(defaultValue = "2") String sortCol
    ) {
        return ApiResponse.success(
                SuccessStatus.HRD_SEARCH_SUCCESS,
                searchService.search(startDt, endDt, page, size, area1, ncs1, sort, sortCol)
        );
    }


    @Operation(
            summary = "교육 상세정보",
            description = "훈련, 훈련기관 상세정보 반환"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세정보 조회 성공")
    })
    @GetMapping("/courses/{trprId}/{trprDegr}")
    public ResponseEntity<ApiResponse<HrdCourseDetailDto>> detail(
            @PathVariable String trprId,
            @PathVariable String trprDegr,
            @RequestParam String torgId
    ) {

        return ApiResponse.success(
                SuccessStatus.HRD_DETAIL_SUCCESS,
                searchService.getDetail(trprId, trprDegr, torgId)
        );
    }


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

    @Operation(
            summary = "교육 상세 + 통계정보",
            description = "훈련과정 상세정보와 통계정보를 함께 반환, 교육 상세정보 페이지에서 활용"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상세 + 통계정보 조회 성공"
            )
    })
    @GetMapping("/courses/{trprId}/{trprDegr}/full")
    public ResponseEntity<ApiResponse<HrdCourseFullDto>> full(
            @PathVariable String trprId,
            @PathVariable String trprDegr,
            @RequestParam String torgId
    ) {
        return ApiResponse.success(
                SuccessStatus.HRD_FULL_SUCCESS,
                searchService.getCourseFull(trprId, trprDegr, torgId)
        );
    }


    @Operation(
            summary = "저장된 훈련과정 목록 조회",
            description = "현재 로그인한 사용자가 즐겨찾기한 HRD 훈련과정 목록을 반환"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "저장된 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedCourseDto>>> listSaved() {
        return ApiResponse.success(
                SuccessStatus.HRD_SAVED_LIST_SUCCESS,
                searchService.listSaved()
        );
    }



    @Operation(
            summary = "훈련과정 즐겨찾기 추가",
            description = "훈련과정을 즐겨찾기 목록에 추가합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "즐겨찾기 추가 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 데이터가 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @PostMapping("/saved")
    public ResponseEntity<ApiResponse<SavedCourseDto>> save(@RequestBody @Valid SaveCourseRequest req) {
        return ApiResponse.success(
                SuccessStatus.HRD_SAVED_ADD_SUCCESS,
                searchService.saveCourse(req)
        );
    }


    @Operation(
            summary = "훈련과정 즐겨찾기 삭제",
            description = "저장된 훈련과정 중 지정한 항목을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "즐겨찾기 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "삭제할 항목을 찾을 수 없음"
            )
    })
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        searchService.deleteSaved(id);
        return ApiResponse.successOnly(SuccessStatus.HRD_SAVED_DELETE_SUCCESS);
    }



    @Operation(
            summary = "즐겨찾기 토글",
            description = "해당 훈련과정이 즐겨찾기에 있으면 삭제하고, 없으면 추가합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "즐겨찾기 상태 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 데이터가 올바르지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @PostMapping("/saved/toggle")
    public ResponseEntity<ApiResponse<SavedCourseDto>> toggle(@RequestBody @Valid SaveCourseRequest req) {
        return ApiResponse.success(
                SuccessStatus.HRD_SAVED_TOGGLE_SUCCESS,
                searchService.toggleSaved(req)
        );
    }

}
