package com.youthjob.api.youthpolicy.controller;

import com.youthjob.api.youthpolicy.dto.SavePolicyRequest;
import com.youthjob.api.youthpolicy.dto.SavedPolicyDto;
import com.youthjob.api.youthpolicy.service.YouthPolicyFavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policies")
public class YouthPolicyFavoriteController {

    private final YouthPolicyFavoriteService service;

    /** 내 관심 정책 목록 */
    @GetMapping("/saved")
    public ResponseEntity<List<SavedPolicyDto>> listSaved() {
        return ResponseEntity.ok(service.listSaved());
    }

    /** 관심 저장 (스냅샷 없으면 서버가 외부 API로 채움) */
    @PostMapping("/saved")
    public ResponseEntity<SavedPolicyDto> save(@RequestBody @Valid SavePolicyRequest req) {
        return ResponseEntity.ok(service.save(req));
    }

    /** 관심 삭제 */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 토글: 있으면 삭제(null 반환), 없으면 저장(저장 DTO 반환) */
    @PostMapping("/saved/toggle")
    public ResponseEntity<SavedPolicyDto> toggle(@RequestBody @Valid SavePolicyRequest req) {
        return ResponseEntity.ok(service.toggle(req));
    }
}
