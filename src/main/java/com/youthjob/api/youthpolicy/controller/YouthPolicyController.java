package com.youthjob.api.youthpolicy.controller;

import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyDetailDto;
import com.youthjob.api.youthpolicy.service.YouthPolicyMapper;
import com.youthjob.api.youthpolicy.service.YouthPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policies")
public class YouthPolicyController {

    private final YouthPolicyService service;
    private final YouthPolicyClient client;

    /** 검색 + 후처리 필터 -> 예시 : /api/v1/youth-policies?plcyKywdNm=보조금&pageNum=1&pageSize=10 */
    @GetMapping
    public YouthPolicyApiResponseDto list(@Valid @ModelAttribute YouthPolicyApiRequestDto req) {
        return service.searchWithFilter(req);
    }

    /** 상세: /api/v1/youth-policies/{plcyNo} */
    @GetMapping("/{plcyNo}")
    public YouthPolicyDetailDto detail(@PathVariable String plcyNo) {
        var resp = client.findByPlcyNo(plcyNo);
        if (resp == null || resp.getResult() == null ||
                resp.getResult().getYouthPolicyList() == null ||
                resp.getResult().getYouthPolicyList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "정책을 찾을 수 없습니다: " + plcyNo);
        }
        var p = resp.getResult().getYouthPolicyList().get(0);
        return YouthPolicyMapper.toDetail(p);
    }
}
