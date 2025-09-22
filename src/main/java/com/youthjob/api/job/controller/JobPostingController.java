// src/main/java/com/youthjob/api/job/controller/JobPostingController.java
package com.youthjob.api.job.controller;

import com.youthjob.api.job.domain.JobPosting;
import com.youthjob.api.job.dto.JobPostingSaveRequest;
import com.youthjob.api.job.service.JobPostingService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.ErrorStatus;
import com.youthjob.common.response.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService service;

    //------ 검색 ------

    /** 검색 + 페이징 */
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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPosting>> getById(@PathVariable Long id) {
        JobPosting jp = service.getById(id);
        if (jp == null) {
            return ApiResponse.failResponse(ErrorStatus.NOT_FOUND_JOB);
        }
        return ApiResponse.success(SuccessStatus.JOB_GET_SUCCESS, jp);
    }

    /** externalId로 단건 조회 */
    @GetMapping("/external/{externalId}")
    public ResponseEntity<ApiResponse<JobPosting>> getByExternalId(@PathVariable String externalId) {
        JobPosting jp = service.getByExternalId(externalId);
        if (jp == null) {
            return ApiResponse.failResponse(ErrorStatus.NOT_FOUND_JOB);
        }
        return ApiResponse.success(SuccessStatus.JOB_GET_SUCCESS, jp);
    }

    /** 저장/업서트 */
    @PostMapping
    public ResponseEntity<ApiResponse<JobPosting>> upsert(@RequestBody JobPostingSaveRequest req) {
        JobPosting saved = service.upsert(req);
        return ApiResponse.success(SuccessStatus.JOB_UPSERT_SUCCESS, saved);
    }

    //--------- 저장 로직 ------------

    /** 저장 */
    @PostMapping("/saved")
    public ResponseEntity<?> save(@AuthenticationPrincipal UserDetails user,
                                  @RequestParam String externalId) {
        boolean created = service.saveForUser(user.getUsername(), externalId);
        if (!created) {
            return ApiResponse.failResponse(ErrorStatus.CONFLICT_SAVED_JOB_ALREADY);
        }
        return ApiResponse.successOnly(SuccessStatus.JOB_SAVED_ADD_SUCCESS);
    }

    /** 토글 */
    @PostMapping("/saved/toggle")
    public ResponseEntity<ApiResponse<Boolean>> toggle(@AuthenticationPrincipal UserDetails user,
                                                       @RequestParam String externalId) {
        boolean nowSaved = service.toggleSave(user.getUsername(), externalId);
        return ApiResponse.success(SuccessStatus.JOB_SAVED_TOGGLE_SUCCESS, nowSaved);
    }

    /** 저장 여부 */
    @GetMapping("/saved/{externalId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(@AuthenticationPrincipal UserDetails user,
                                                       @PathVariable String externalId) {
        boolean exists = service.isSaved(user.getUsername(), externalId);
        return ApiResponse.success(SuccessStatus.JOB_SAVED_EXISTS_SUCCESS, exists);
    }

    /** 저장 목록 */
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<Page<JobPosting>>> list(@AuthenticationPrincipal UserDetails user,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        Page<JobPosting> page = service.listSavedJobs(user.getUsername(), pageable);
        return ApiResponse.success(SuccessStatus.JOB_SAVED_LIST_SUCCESS, page);
    }

    /** 저장 삭제 */
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
