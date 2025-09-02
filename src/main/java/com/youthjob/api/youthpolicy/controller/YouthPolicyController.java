package com.youthjob.api.youthpolicy.controller;

import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyDetailDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policies")
public class YouthPolicyController {

    private final YouthPolicyClient client;

    /** api 검색, 호출 예: /api/v1/youth-policies?plcyKywdNm=보조금&pageNum=1&pageSize=10 */
    @GetMapping
    public YouthPolicyApiResponseDto list(@Valid @ModelAttribute YouthPolicyApiRequestDto req) {
        return client.search(req);
    }

    /** 각 정책별 상세정보: /api/v1/youth-policies/{plcyNo} */
    @GetMapping("/{plcyNo}")
    public YouthPolicyDetailDto detail(@PathVariable String plcyNo) {
        var resp = client.findByPlcyNo(plcyNo);
        if (resp == null || resp.getResult() == null ||
                resp.getResult().getYouthPolicyList() == null ||
                resp.getResult().getYouthPolicyList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "정책을 찾을 수 없습니다: " + plcyNo);
        }

        var p = resp.getResult().getYouthPolicyList().get(0);

        // 뷰 DTO로 요약 반환 (프론트 필요한 정보 보고 수정할 예정)
        return YouthPolicyDetailDto.builder()
                .plcyNo(p.getPlcyNo())
                .plcyNm(p.getPlcyNm())
                .plcyKywdNm(p.getPlcyKywdNm())
                .plcyExplnCn(p.getPlcyExplnCn())
                .lclsfNm(p.getLclsfNm())
                .mclsfNm(p.getMclsfNm())
                .aplyYmd(p.getAplyYmd())
                .aplyUrlAddr(p.getAplyUrlAddr())
                .sprvsnInstCdNm(p.getSprvsnInstCdNm())
                .operInstCdNm(p.getOperInstCdNm())
                .build();
    }
}
