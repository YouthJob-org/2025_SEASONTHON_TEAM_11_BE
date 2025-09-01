package com.youthjob.api.hrd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SaveCourseRequest(
        @NotBlank String trprId,
        @NotBlank String trprDegr,
        @NotBlank String title,
        String subTitle,
        String address,
        String telNo,
        String traStartDate,
        String traEndDate,
        String trainTarget,
        String trainTargetCd,
        String ncsCd,
        String courseMan,
        String realMan,
        String yardMan,
        String titleLink,
        String subTitleLink
) {}