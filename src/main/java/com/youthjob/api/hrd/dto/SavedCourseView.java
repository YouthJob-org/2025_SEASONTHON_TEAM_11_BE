package com.youthjob.api.hrd.dto;

import lombok.Builder;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record SavedCourseView(
        Long id,
        String trprId, String trprDegr, String torgId,
        String title, String subTitle,
        String address, String telNo,
        LocalDate traStartDate, LocalDate traEndDate,
        String trainTarget, String trainTargetCd,
        String ncsCd,
        String courseMan, String realMan, String yardMan,
        String titleLink, String subTitleLink,
        LocalDateTime createdAt
) {}

