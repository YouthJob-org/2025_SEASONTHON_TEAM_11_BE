package com.youthjob.api.youthpolicy.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record YouthPolicyDetailDto(
        // === 기본 요약 ===
        String plcyNo,
        String plcyNm,
        String summary,                 // plcyExplnCn
        List<String> keywords,          // plcyKywdNm (콤마 분리)
        String categoryL,               // lclsfNm
        String categoryM,               // mclsfNm

        // === 신청/기간/방법 ===
        String applyPeriodType,         // aplyPrdSeCd 라벨(특정기간/상시/마감)
        String applyPeriod,             // aplyYmd
        String applyUrl,                // aplyUrlAddr
        String applyMethod,             // plcyAplyMthdCn

        // === 지원내용/선착순/규모 ===
        String supportContent,          // plcySprtCn
        Boolean firstCome,              // sprtArvlSeqYn == 'Y'
        Boolean supportScaleLimited,    // sprtSclLmtYn == 'Y'
        Integer supportCount,           // sprtSclCnt

        // === 자격요건 ===
        Integer ageMin,                 // sprtTrgtMinAge
        Integer ageMax,                 // sprtTrgtMaxAge
        Boolean ageLimited,             // sprtTrgtAgeLmtYn == 'Y'
        String maritalStatus,           // mrgSttsCd 라벨
        String incomeConditionType,     // earnCndSeCd 라벨
        String incomeMin,               // earnMinAmt
        String incomeMax,               // earnMaxAmt
        String incomeEtc,               // earnEtcCn
        List<String> jobRequirements,   // jobCd 라벨 리스트
        String majorRequirement,        // plcyMajorCd 라벨
        List<String> schoolRequirements,// schoolCd 라벨 리스트
        List<String> specialRequirements,// sbizCd 라벨 리스트
        String additionalQualification, // addAplyQlfcCndCn
        String participantTarget,       // ptcpPrpTrgtCn

        // === 심사/제출/비고 ===
        String screeningMethod,         // srngMthdCn
        String requiredDocs,            // sbmsnDcmntCn
        String etcNotes,                // etcMttrCn
        List<String> refUrls,           // refUrlAddr1/2

        // === 기관/담당 ===
        String providerGroup,           // pvsnInstGroupCd 라벨
        String provisionMethod,         // plcyPvsnMthdCd 라벨
        String supervisorName,          // sprvsnInstCdNm
        String operatorName,            // operInstCdNm
        String supervisorContactName,   // sprvsnInstPicNm
        String operatorContactName,     // operInstPicNm

        // === 사업기간(참고) ===
        String businessPeriodType,      // bizPrdSeCd 라벨
        String businessBeginYmd,        // bizPrdBgngYmd(YYYY-MM-DD)
        String businessEndYmd,          // bizPrdEndYmd(YYYY-MM-DD)
        String businessEtc,             // bizPrdEtcCn

        // === 지역/기타 ===
        List<String> zipCodes,          // zipCd (콤마 분리)
        Integer viewCount,              // inqCnt
        String firstRegisteredAt,       // frstRegDt
        String lastModifiedAt           // lastMdfcnDt
) {}
