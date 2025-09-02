package com.youthjob.api.youthpolicy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class YouthPolicyApiResponseDto {
    private int resultCode;
    private String resultMessage;
    private Result result;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Result {
        private Pagging pagging;
        private List<Policy> youthPolicyList;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Pagging {
        private Integer totCount;
        private Integer pageNum;
        private Integer pageSize;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Policy {
        private String plcyNo;
        private String bscPlanCycl;
        private String bscPlanPlcyWayNo;
        private String bscPlanFcsAsmtNo;
        private String bscPlanAsmtNo;
        private String pvsnInstGroupCd;
        private String plcyPvsnMthdCd;
        private String plcyAprvSttsCd;
        private String plcyNm;
        private String plcyKywdNm;
        private String plcyExplnCn;
        private String lclsfNm;
        private String mclsfNm;
        private String plcySprtCn;
        private String sprvsnInstCd;
        private String sprvsnInstCdNm;
        private String sprvsnInstPicNm;
        private String operInstCd;
        private String operInstCdNm;
        private String operInstPicNm;
        private String sprtSclLmtYn;
        private String aplyPrdSeCd;
        private String bizPrdSeCd;
        private String bizPrdBgngYmd;
        private String bizPrdEndYmd;
        private String bizPrdEtcCn;
        private String plcyAplyMthdCn;
        private String srngMthdCn;
        private String aplyUrlAddr;
        private String sbmsnDcmntCn;
        private String etcMttrCn;
        private String refUrlAddr1;
        private String refUrlAddr2;
        private String sprtSclCnt;
        private String sprtArvlSeqYn;
        private String sprtTrgtMinAge;
        private String sprtTrgtMaxAge;
        private String sprtTrgtAgeLmtYn;
        private String mrgSttsCd;
        private String earnCndSeCd;
        private String earnMinAmt;
        private String earnMaxAmt;
        private String earnEtcCn;
        private String addAplyQlfcCndCn;
        private String ptcpPrpTrgtCn;
        private String inqCnt;
        private String rgtrInstCd;
        private String rgtrInstCdNm;
        private String rgtrUpInstCd;
        private String rgtrUpInstCdNm;
        private String rgtrHghrkInstCd;
        private String rgtrHghrkInstCdNm;
        private String zipCd;
        private String plcyMajorCd;
        private String jobCd;
        private String schoolCd;
        private String aplyYmd;
        private String frstRegDt;
        private String lastMdfcnDt;
        private String sbizCd;
    }
}
