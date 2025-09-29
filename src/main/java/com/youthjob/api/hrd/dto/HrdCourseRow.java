package com.youthjob.api.hrd.dto;

import java.time.LocalDate;

public interface HrdCourseRow {
    String getTitle();
    String getSubTitle();
    String getAddress();
    String getTelNo();
    LocalDate getTraStartDate();
    LocalDate getTraEndDate();
    String getTrainTarget();
    String getTrainTargetCd();
    String getNcsCd();
    String getTrprId();
    String getTrprDegr();
    String getCourseMan();
    String getRealMan();
    String getYardMan();
    String getTitleLink();
    String getSubTitleLink();
    String getTorgId();
}