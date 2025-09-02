package com.youthjob.api.mypage.dto;

import lombok.Builder;

@Builder
public record MyPageSummaryDto(
        ProfileDto profile,
        CountersDto counters
) {}
