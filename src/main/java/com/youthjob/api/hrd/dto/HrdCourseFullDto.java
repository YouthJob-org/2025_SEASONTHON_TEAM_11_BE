package com.youthjob.api.hrd.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrdCourseFullDto {
    private HrdCourseDetailDto detail;         // 310L02 파싱 결과
    private List<HrdCourseStatDto> stats;      // 310L03 파싱 결과(0~N개)
}