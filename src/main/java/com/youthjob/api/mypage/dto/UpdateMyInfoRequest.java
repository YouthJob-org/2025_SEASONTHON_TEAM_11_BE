package com.youthjob.api.mypage.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateMyInfoRequest(
        @Size(min = 1, max = 30) String name
) {}
