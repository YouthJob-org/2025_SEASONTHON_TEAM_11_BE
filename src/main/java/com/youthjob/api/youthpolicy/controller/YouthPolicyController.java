package com.youthjob.api.youthpolicy.controller;

import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyDetailDto;
import com.youthjob.api.youthpolicy.service.YouthPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policies")
public class YouthPolicyController {

    private final YouthPolicyService service;

    /** DB 적재(초기/갱신) - 외부 API 전체 수집해서 DB에 upsert */
    @PostMapping("/db")
    public ResponseEntity<String> ingest(@RequestParam(defaultValue = "true") boolean full) {
        long count = service.ingestAll(full);
        return ResponseEntity.ok("성공!, 총 " + count + "개의 청년정책 API 연동이 완료되었습니다.");
    }

    /** 검색: DB 기반 */
    @GetMapping
    public YouthPolicyApiResponseDto list(@Valid @ModelAttribute YouthPolicyApiRequestDto req) {
        return service.searchFromDb(req);
    }

    /** 상세: DB 기반 */
    @GetMapping("/{plcyNo}")
    public YouthPolicyDetailDto detail(@PathVariable String plcyNo) {
        return service.detailFromDb(plcyNo);
    }
}
