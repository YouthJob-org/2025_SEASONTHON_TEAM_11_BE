package com.youthjob.api.youthpolicy.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class YouthPolicyApiRequestDto {
    // 기본/필수
    @Builder.Default @Min(1) private Integer pageNum  = 1;
    @Builder.Default @Min(1) private Integer pageSize = 10;
    @Builder.Default private Integer pageType = 1;     // 1=목록, 2=상세
    @Builder.Default private String  rtnType  = "json";

    // 선택 필터
    private String plcyNo;       // 정책번호
    private String plcyKywdNm;   // "키워드1,키워드2"
    private String plcyExpInCn;  // 정책설명
    private String plcyNm;       // 정책명
    private String zipCd;        // "11680,41135"
    private String lclsfNm;      // 대분류명
    private String mclsfNm;      // 중분류명
}
