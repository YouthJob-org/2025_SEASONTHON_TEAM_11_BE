package com.youthjob.api.youthpolicy.dto;

import jakarta.validation.constraints.NotBlank;

public record SavePolicyRequest(
        @NotBlank String plcyNo,
        // ====== 스냅샷 : 없으면 서버가 외부 API 호출 후 직접 채움 ======
        String plcyNm,
        String plcyKywdNm,
        String lclsfNm,
        String mclsfNm,
        String aplyYmd,
        String aplyUrlAddr,
        String sprvsnInstCdNm,
        String operInstCdNm
) {}
