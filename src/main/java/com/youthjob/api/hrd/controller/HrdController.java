package com.youthjob.api.hrd.controller;

import com.youthjob.api.hrd.dto.HrdCourseDetailDto;
import com.youthjob.api.hrd.dto.HrdCourseDto;
import com.youthjob.api.hrd.dto.SaveCourseRequest;
import com.youthjob.api.hrd.dto.SavedCourseDto;
import com.youthjob.api.hrd.service.HrdSearchService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.SuccessStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hrd")
public class HrdController {

    private final HrdSearchService searchService;

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
        return ResponseEntity.ok(searchService.search(startDt, endDt, page, size, area1, ncs1, sort, sortCol));
    }

    // ====== 저장(관심) 기능 ======
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedCourseDto>>> listSaved() {
        return ApiResponse.success(SuccessStatus.HRD_SAVED_LIST_SUCCESS, searchService.listSaved());
    }

    @PostMapping("/saved")
    public ResponseEntity<ApiResponse<SavedCourseDto>> save(@RequestBody @Valid SaveCourseRequest req) {
        return ApiResponse.success(SuccessStatus.HRD_SAVED_ADD_SUCCESS, searchService.saveCourse(req));
    }

    @DeleteMapping("/saved/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        searchService.deleteSaved(id);
        return ApiResponse.successOnly(SuccessStatus.HRD_SAVED_DELETE_SUCCESS);
    }

    @PostMapping("/saved/toggle")
    public ResponseEntity<ApiResponse<SavedCourseDto>> toggle(@RequestBody @Valid SaveCourseRequest req) {
        return ApiResponse.success(SuccessStatus.HRD_SAVED_TOGGLE_SUCCESS, searchService.toggleSaved(req));
    }

    @GetMapping("/courses/{trprId}/{trprDegr}")
    public ResponseEntity<HrdCourseDetailDto> detail(
            @PathVariable String trprId,
            @PathVariable String trprDegr,
            @RequestParam String torgId
    ) {
        return ResponseEntity.ok(searchService.getDetail(trprId, trprDegr, torgId));
    }
}