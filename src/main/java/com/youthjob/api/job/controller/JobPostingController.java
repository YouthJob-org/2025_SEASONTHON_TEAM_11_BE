package com.youthjob.api.job.controller;

import com.youthjob.api.job.domain.JobPosting;
import com.youthjob.api.job.dto.JobPostingSaveRequest;
import com.youthjob.api.job.service.JobPostingService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.ErrorStatus;
import com.youthjob.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "HRD", description = "채용 정보 검색, 저장 관련 API 입니다.")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService service;

    /** 검색 + 페이징 */
    @Operation(summary = "채용 공고 검색",
            description = "키워드, 지역, 등록일/마감일 범위로 검색")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobPosting>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ddlFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ddlTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<JobPosting> page = service.search(q, region, regFrom, regTo, ddlFrom, ddlTo, pageable);
        return ApiResponse.success(SuccessStatus.JOB_SEARCH_SUCCESS, page);
    }

    /** ID로 단건 조회 */
    @Operation(summary = "채용 공고 단건 조회", description = "ID로 채용 공고 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPosting>> getById(@PathVariable Long id) {
        JobPosting jp = service.getById(id);
        if (jp == null) {
            return ApiResponse.failResponse(ErrorStatus.NOT_FOUND_JOB);
        }
        return ApiResponse.success(SuccessStatus.JOB_GET_SUCCESS, jp);
    }

    /** externalId로 단건 조회 */
    @Operation(summary = "채용 공고 단건 조회", description = "ID로 채용 공고 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않음")
    })
    @GetMapping("/external/{externalId}")
    public ResponseEntity<ApiResponse<JobPosting>> getByExternalId(@PathVariable String externalId) {
        JobPosting jp = service.getByExternalId(externalId);
        if (jp == null) {
            return ApiResponse.failResponse(ErrorStatus.NOT_FOUND_JOB);
        }
        return ApiResponse.success(SuccessStatus.JOB_GET_SUCCESS, jp);
    }


    @Operation(summary = "채용 공고 저장/업서트", description = "요청 본문 기준으로 공고를 생성 또는 갱신")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업서트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<JobPosting>> upsert(@RequestBody JobPostingSaveRequest req) {
        JobPosting saved = service.upsert(req);
        return ApiResponse.success(SuccessStatus.JOB_UPSERT_SUCCESS, saved);
    }

    //--------- 저장 로직 ------------
    @Operation(summary = "채용 공고 저장", description = "채용 공고 저장")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/saved")
    public ResponseEntity<?> save(@AuthenticationPrincipal UserDetails user,
                                  @RequestParam String externalId) {
        boolean created = service.saveForUser(user.getUsername(), externalId);
        if (!created) {
            return ApiResponse.failResponse(ErrorStatus.CONFLICT_SAVED_JOB_ALREADY);
        }
        return ApiResponse.successOnly(SuccessStatus.JOB_SAVED_ADD_SUCCESS);
    }

    @Operation(summary = "채용 공고 저장 토글", description = "채용 공고 저장 토글")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/saved/toggle")
    public ResponseEntity<ApiResponse<Boolean>> toggle(@AuthenticationPrincipal UserDetails user,
                                                       @RequestParam String externalId) {
        boolean nowSaved = service.toggleSave(user.getUsername(), externalId);
        return ApiResponse.success(SuccessStatus.JOB_SAVED_TOGGLE_SUCCESS, nowSaved);
    }

    @Operation(summary = "저장 여부", description = "채용 공고 저장 여부 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/saved/{externalId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(@AuthenticationPrincipal UserDetails user,
                                                       @PathVariable String externalId) {
        boolean exists = service.isSaved(user.getUsername(), externalId);
        return ApiResponse.success(SuccessStatus.JOB_SAVED_EXISTS_SUCCESS, exists);
    }

    @Operation(summary = "저장 목록", description = "저장된 채용 공고 목록을 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<Page<JobPosting>>> list(@AuthenticationPrincipal UserDetails user,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        Page<JobPosting> page = service.listSavedJobs(user.getUsername(), pageable);
        return ApiResponse.success(SuccessStatus.JOB_SAVED_LIST_SUCCESS, page);
    }

    @Operation(summary = "저장 취소", description = "채용 공고 저장 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @DeleteMapping("/saved/{externalId}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails user,
                                    @PathVariable String externalId) {
        boolean removed = service.removeSaved(user.getUsername(), externalId);
        if (!removed) {
            return ApiResponse.failResponse(ErrorStatus.NOT_FOUND_SAVED_JOB);
        }
        return ApiResponse.successOnly(SuccessStatus.JOB_SAVED_DELETE_SUCCESS);
    }
}
