package com.youthjob.api.mypage.dto;

import lombok.Builder;

@Builder
public record CountersDto(
        long savedEmpPrograms,   // 취업역량 강화프로그램
        long savedCourses,       // 내일배움카드
        long savedPolicies       // 청년정책
) {}
