package com.youthjob.api.mypage.dto;

import lombok.Builder;

@Builder
public record CountersDto(
       //  long savedCapabilities,  // 취업역량 강화
        long savedCourses,       // 내일배움카드
        long savedPolicies       // 청년정책
) {}
