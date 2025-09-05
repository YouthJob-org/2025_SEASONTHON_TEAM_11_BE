package com.youthjob.api.youthpolicy.service;

import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto.Policy;
import com.youthjob.api.youthpolicy.dto.YouthPolicyDetailDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class YouthPolicyMapper {

    private static final DateTimeFormatter API_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OUT_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private YouthPolicyMapper(){}

    public static YouthPolicyDetailDto toDetail(Policy p){
        return YouthPolicyDetailDto.builder()
                // 기본
                .plcyNo(p.getPlcyNo())
                .plcyNm(nz(p.getPlcyNm()))
                .summary(nz(p.getPlcyExplnCn()))
                .keywords(splitCsv(nz(p.getPlcyKywdNm())))
                .categoryL(nz(p.getLclsfNm()))
                .categoryM(nz(p.getMclsfNm()))
                // 신청/기간/방법
                .applyPeriodType(CodeDict.label(CodeDict.APPLY_PERIOD, p.getAplyPrdSeCd()))
                .applyPeriod(normalizeAplyYmd(p.getAplyYmd()))
                .applyUrl(nz(p.getAplyUrlAddr()))
                .applyMethod(nz(p.getPlcyAplyMthdCn()))
                // 지원내용/선착순/규모
                .supportContent(nz(p.getPlcySprtCn()))
                .firstCome("Y".equalsIgnoreCase(nz(p.getSprtArvlSeqYn())))
                .supportScaleLimited("Y".equalsIgnoreCase(nz(p.getSprtSclLmtYn())))
                .supportCount(parseIntSafe(p.getSprtSclCnt()))
                // 자격요건
                .ageMin(parseIntSafe(p.getSprtTrgtMinAge()))
                .ageMax(parseIntSafe(p.getSprtTrgtMaxAge()))
                .ageLimited("Y".equalsIgnoreCase(nz(p.getSprtTrgtAgeLmtYn())))
                .maritalStatus(CodeDict.label(CodeDict.MARITAL, p.getMrgSttsCd()))
                .incomeConditionType(CodeDict.label(CodeDict.INCOME, p.getEarnCndSeCd()))
                .incomeMin(nz(p.getEarnMinAmt()))
                .incomeMax(nz(p.getEarnMaxAmt()))
                .incomeEtc(nz(p.getEarnEtcCn()))
                .jobRequirements(CodeDict.decodeCsv(CodeDict.JOB, p.getJobCd()))
                .majorRequirement(CodeDict.label(CodeDict.MAJOR, p.getPlcyMajorCd()))
                .schoolRequirements(CodeDict.decodeCsv(CodeDict.SCHOOL, p.getSchoolCd()))
                .specialRequirements(CodeDict.decodeCsv(CodeDict.SBIZ, p.getSbizCd()))
                .additionalQualification(nz(p.getAddAplyQlfcCndCn()))
                .participantTarget(nz(p.getPtcpPrpTrgtCn()))
                // 심사/제출/비고
                .screeningMethod(nz(p.getSrngMthdCn()))
                .requiredDocs(nz(p.getSbmsnDcmntCn()))
                .etcNotes(nz(p.getEtcMttrCn()))
                .refUrls(refUrls(p.getRefUrlAddr1(), p.getRefUrlAddr2()))
                // 기관/담당
                .providerGroup(CodeDict.label(CodeDict.PVSN_INST_GROUP, p.getPvsnInstGroupCd()))
                .provisionMethod(CodeDict.label(CodeDict.PVSN_METHOD, p.getPlcyPvsnMthdCd()))
                .supervisorName(nz(p.getSprvsnInstCdNm()))
                .operatorName(nz(p.getOperInstCdNm()))
                .supervisorContactName(nz(p.getSprvsnInstPicNm()))
                .operatorContactName(nz(p.getOperInstPicNm()))
                // 사업기간
                .businessPeriodType(CodeDict.label(CodeDict.BIZ_PERIOD, p.getBizPrdSeCd()))
                .businessBeginYmd(formatYmd(p.getBizPrdBgngYmd()))
                .businessEndYmd(formatYmd(p.getBizPrdEndYmd()))
                .businessEtc(nz(p.getBizPrdEtcCn()))
                // 지역/기타
                .zipCodes(splitCsv(nz(p.getZipCd())))
                .viewCount(parseIntSafe(p.getInqCnt()))
                .firstRegisteredAt(nz(p.getFrstRegDt()))
                .lastModifiedAt(nz(p.getLastMdfcnDt()))
                .build();
    }

    // ===== helpers =====
    private static String nz(String s){ return (s==null || s.isBlank()) ? "" : s.trim(); }

    private static List<String> splitCsv(String csv){
        if (csv.isEmpty()) return List.of();
        String[] parts = csv.split("\\s*,\\s*");
        List<String> out = new ArrayList<>();
        for (String p: parts) if (!p.isBlank()) out.add(p);
        return out;
    }

    private static String formatYmd(String ymd){
        String v = nz(ymd).replaceAll("\\s+", "");
        if (v.length()!=8) return v.isEmpty() ? null : v;
        try { return LocalDate.parse(v, API_YMD).format(OUT_YMD); }
        catch(Exception e){ return v; }
    }

    private static String normalizeAplyYmd(String raw){
        // "20250430 ~ 20250627" / "20250805 ~ 20250822\\N20250301 ~ 20251231"
        String v = nz(raw);
        if (v.isEmpty()) return null;
        return v.replace("\\N", "\n");
    }

    private static Integer parseIntSafe(String s){
        String v = nz(s);
        if (v.isEmpty()) return null;
        try { return Integer.parseInt(v); } catch(Exception e){ return null; }
    }

    private static List<String> refUrls(String u1, String u2){
        List<String> out = new ArrayList<>();
        if (!nz(u1).isEmpty()) out.add(u1.trim());
        if (!nz(u2).isEmpty()) out.add(u2.trim());
        return out;
    }
}
