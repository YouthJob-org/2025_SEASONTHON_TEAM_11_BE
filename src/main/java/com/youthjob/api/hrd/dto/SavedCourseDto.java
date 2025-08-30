package com.youthjob.api.hrd.dto;

import com.youthjob.api.hrd.domain.SavedCourse;

public record SavedCourseDto(
        Long id,
        String trprId,
        String trprDegr,
        String title,
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
) {
    public static SavedCourseDto from(SavedCourse s) {
        return new SavedCourseDto(
                s.getId(),
                s.getTrprId(),
                s.getTrprDegr(),
                s.getTitle(),
                s.getSubTitle(),
                s.getAddress(),
                s.getTelNo(),
                s.getTraStartDate(),
                s.getTraEndDate(),
                s.getTrainTarget(),
                s.getTrainTargetCd(),
                s.getNcsCd(),
                s.getCourseMan(),
                s.getRealMan(),
                s.getYardMan(),
                s.getTitleLink(),
                s.getSubTitleLink()
        );
    }
}
