package com.youthjob.api.youthpolicy.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
public class YouthPolicyApiRequestDto {

    // 기본값
    private String rtnType = "json";
    @Min(1) private Integer pageNum = 1;
    @Min(1) private Integer pageSize = 10;
    /** 화면유형: "1"(목록) / "2"(상세) - 스펙은 String */
    private String pageType = "1";

    // 선택 필터
    private String plcyNo;
    private String plcyKywdNm;
    private String plcyNm;
    /** 정책설명(요청 파라미터 키: plcyExpInCn) */
    private String plcyExpInCn;
    private String zipCd;
    private String lclsfNm;
    private String mclsfNm;

    // --- 바인딩용 최소 setter들 ---
    public void setRtnType(String v)     { if (notBlank(v)) this.rtnType = v; }
    public void setPageNum(Integer v)    { if (v != null && v > 0) this.pageNum = v; }
    public void setPageSize(Integer v)   { if (v != null && v > 0) this.pageSize = v; }
    public void setPageType(String v)    { if (notBlank(v)) this.pageType = v; }
    public void setPlcyNo(String v)      { this.plcyNo = v; }
    public void setPlcyKywdNm(String v)  { this.plcyKywdNm = v; }
    public void setPlcyNm(String v)      { this.plcyNm = v; }
    public void setPlcyExpInCn(String v) { this.plcyExpInCn = v; }
    public void setZipCd(String v)       { this.zipCd = v; }
    public void setLclsfNm(String v)     { this.lclsfNm = v; }
    public void setMclsfNm(String v)     { this.mclsfNm = v; }

    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }

    /** 외부 API 호출용 파라미터 세트 */
    public MultiValueMap<String, String> toQueryParams(String apiKey){
        MultiValueMap<String, String> p = new LinkedMultiValueMap<>();
        // 필수
        p.add("apiKeyNm", apiKey);
        p.add("rtnType", rtnType);
        p.add("pageNum", String.valueOf(pageNum));
        p.add("pageSize", String.valueOf(pageSize));
        p.add("pageType", pageType); // String
        // 선택
        add(p, "plcyNo", plcyNo);
        add(p, "plcyKywdNm", plcyKywdNm);
        add(p, "plcyNm", plcyNm);
        add(p, "plcyExpInCn", plcyExpInCn); // <-- 요청 키는 ExpIn
        add(p, "zipCd", zipCd);
        add(p, "lclsfNm", lclsfNm);
        add(p, "mclsfNm", mclsfNm);
        return p;
    }
    private static void add(MultiValueMap<String, String> map, String k, String v){
        if (v != null && !v.isBlank()) map.add(k, v);
    }
}
