package com.youthjob.api.youthpolicy.dto;

import lombok.Builder;

@Builder
public record YouthPolicyDetailDto(
        String plcyNo,
        String plcyNm,
        String plcyKywdNm,
        String plcyExplnCn,
        String lclsfNm,
        String mclsfNm,
        String aplyYmd,
        String aplyUrlAddr,
        String sprvsnInstCdNm,
        String operInstCdNm
) {}
