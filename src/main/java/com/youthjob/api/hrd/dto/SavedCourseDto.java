package com.youthjob.api.hrd.dto;

import com.youthjob.api.hrd.domain.SavedCourse;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SavedCourseDto(
        Long id,
        String trprId, String trprDegr,
        String title, String subTitle,
        String address, String telNo,
        String traStartDate, String traEndDate,
        String trainTarget, String trainTargetCd,
        String ncsCd,
        String courseMan, String realMan, String yardMan,
        String titleLink, String subTitleLink,
        Instant createdAt
) {
    public static SavedCourseDto from(SavedCourse s) {
        return SavedCourseDto.builder()
                .id(s.getId())
                .trprId(s.getTrprId())
                .trprDegr(s.getTrprDegr())
                .title(s.getTitle())
                .subTitle(s.getSubTitle())
                .address(s.getAddress())
                .telNo(s.getTelNo())
                .traStartDate(s.getTraStartDate())
                .traEndDate(s.getTraEndDate())
                .trainTarget(s.getTrainTarget())
                .trainTargetCd(s.getTrainTargetCd())
                .ncsCd(s.getNcsCd())
                .courseMan(s.getCourseMan())
                .realMan(s.getRealMan())
                .yardMan(s.getYardMan())
                .titleLink(s.getTitleLink())
                .subTitleLink(s.getSubTitleLink())
                .build();
    }
}