package com.youthjob.api.hrd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SaveCourseRequest(
        @NotBlank String trprId,
        @NotBlank String trprDegr,
        String torgId
) {}
