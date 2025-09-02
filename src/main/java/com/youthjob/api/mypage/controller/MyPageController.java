package com.youthjob.api.mypage.controller;

import com.youthjob.api.hrd.dto.SavedCourseDto;
import com.youthjob.api.mypage.dto.*;
import com.youthjob.api.mypage.service.MyPageService;
import com.youthjob.api.youthpolicy.dto.SavedPolicyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageController {

    private final MyPageService service;

    /** 상단 프로필 + 카운트 */
    @GetMapping("/summary")
    public ResponseEntity<MyPageSummaryDto> summary() {
        return ResponseEntity.ok(service.summary());
    }

    /** 내 정보 단독 조회 (수정 화면 초기값) */
    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> profile() {
        return ResponseEntity.ok(service.profile());
    }

    /** 내 정보 수정: 이름 */
    @PutMapping("/profile")
    public ResponseEntity<ProfileDto> updateProfile(@RequestBody @Valid UpdateMyInfoRequest req) {
        return ResponseEntity.ok(service.updateProfile(req));
    }

//    /** 관심 - 취업역량 강화 프로그램 */
//    @GetMapping("/saved-capabilities")
//    public ResponseEntity<PageResult<SavedCapabilityDto>> savedCapabilities(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(service.savedCapabilities(page, size));
//    }

    /** 관심 - 내일배움카드 */
    @GetMapping("/saved-courses")
    public ResponseEntity<PageResult<SavedCourseDto>> savedCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.savedCourses(page, size));
    }

    /** 관심 - 청년정책 */
    @GetMapping("/saved-policies")
    public ResponseEntity<PageResult<SavedPolicyDto>> savedPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.savedPolicies(page, size));
    }
}
