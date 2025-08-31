package com.youthjob.api.hrd.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record HrdCourseDetailDto(
        // 기관/과정 기본
        String trprId, String trprDegr, String trprNm,
        String ncsCd, String ncsNm, String traingMthCd,
        String trtm, String trDcnt, String perTrco, String instPerTrco,

        // 기관 연락/주소
        String inoNm, String addr1, String addr2, String zipCd,
        String hpAddr, String trprChap, String trprChapTel, String trprChapEmail,

        // 리스트들
        List<Facility> facilities,
        List<Equipment> equipments
) {
    @Builder public record Facility(String cstmrId, String trafcltyNm, String ocuAcptnNmprCn, String holdQy, String fcltyArCn) {}
    @Builder public record Equipment(String cstmrNm, String eqpmnNm, String holdQy) {}
}