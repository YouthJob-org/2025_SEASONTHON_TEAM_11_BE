package com.youthjob.api.hrd.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HrdCourseDto {
    private String title;
    private String subTitle;
    private String address;
    private String telNo;
    private String traStartDate;
    private String traEndDate;
    private String trainTarget;
    private String trainTargetCd;
    private String ncsCd;
    private String trprId;      // 과정ID (PK 후보)
    private String trprDegr;    // 순차 (복합PK 후보)
    private String courseMan;   // 수강비
    private String realMan;     // 실제훈련비
    private String yardMan;     // 정원
    private String titleLink;   // 상세 링크
    private String subTitleLink;
    private String torgId;
}