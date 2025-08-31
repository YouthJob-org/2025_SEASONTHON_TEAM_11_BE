package com.youthjob.api.hrd.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HrdCourseStatDto {
    String trprId;        // 훈련과정ID
    String trprDegr;      // 회차(옵션)
    String trprNm;        // 과정명
    String instIno;       // 훈련기관 코드

    String trStaDt;       // 시작일
    String trEndDt;       // 종료일

    String totFxnum;      // 모집정원
    String totParMks;     // 수강인원
    String totTrpCnt;     // 수강(신청)인원
    String finiCnt;       // 수료인원
    String totTrco;       // 총 훈련비

    String eiEmplRate3;   // 3개월 고용보험 취업률
    String eiEmplCnt3;    // 3개월 고용보험 취업인원
    String eiEmplRate6;   // 6개월 고용보험 취업률
    String eiEmplCnt6;    // 6개월 고용보험 취업인원
    String hrdEmplRate6;  // 6개월 고용보험 미가입 취업률
    String hrdEmplCnt6;   // 6개월 고용보험 미가입 취업인원
}
