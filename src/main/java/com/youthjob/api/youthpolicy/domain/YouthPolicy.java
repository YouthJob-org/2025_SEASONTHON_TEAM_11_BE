package com.youthjob.api.youthpolicy.domain;

import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto.Policy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "youth_policy",
        indexes = {
                @Index(name = "idx_youth_policy_plcyno", columnList = "plcy_no", unique = true),
                @Index(name = "idx_youth_policy_plcynm", columnList = "plcy_nm"),
                @Index(name = "idx_youth_policy_zipcd", columnList = "zip_cd"),
                @Index(name = "idx_youth_policy_lclsf", columnList = "lclsf_nm"),
                @Index(name = "idx_youth_policy_mclsf", columnList = "mclsf_nm")
        }
)
@EqualsAndHashCode(of = "plcyNo")
public class YouthPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plcy_no", nullable = false, length = 40, unique = true)
    private String plcyNo;

    @Column(name = "plcy_nm", length = 300) private String plcyNm;
    @Column(name = "plcy_kywd_nm", length = 500) private String plcyKywdNm;

    @Lob @Column(name = "plcy_expln_cn", columnDefinition = "LONGTEXT") private String plcyExplnCn;

    @Column(name = "lclsf_nm", length = 50) private String lclsfNm;
    @Column(name = "mclsf_nm", length = 50) private String mclsfNm;

    @Lob @Column(name = "plcy_sprt_cn", columnDefinition = "LONGTEXT") private String plcySprtCn;
    @Column(name = "sprt_arvl_seq_yn", length = 1) private String sprtArvlSeqYn;
    @Column(name = "sprt_scl_lmt_yn", length = 1) private String sprtSclLmtYn;
    @Column(name = "sprt_scl_cnt", length = 20) private String sprtSclCnt;

    @Column(name = "aply_prd_se_cd", length = 20) private String aplyPrdSeCd;
    @Column(name = "aply_ymd", length = 500) private String aplyYmd;
    @Column(name = "aply_url_addr", length = 1000) private String aplyUrlAddr;
    @Lob @Column(name = "plcy_aply_mthd_cn", columnDefinition = "LONGTEXT") private String plcyAplyMthdCn;

    @Column(name = "sprvsn_inst_cd_nm", length = 200) private String sprvsnInstCdNm;
    @Column(name = "oper_inst_cd_nm", length = 200) private String operInstCdNm;

    // 자격요건 요약
    @Column(name = "sprt_trgt_min_age", length = 5) private String sprtTrgtMinAge;
    @Column(name = "sprt_trgt_max_age", length = 5) private String sprtTrgtMaxAge;
    @Column(name = "sprt_trgt_age_lmt_yn", length = 1) private String sprtTrgtAgeLmtYn;
    @Column(name = "mrg_stts_cd", length = 20) private String mrgSttsCd;
    @Column(name = "earn_cnd_se_cd", length = 20) private String earnCndSeCd;
    @Column(name = "earn_min_amt", length = 30) private String earnMinAmt;
    @Column(name = "earn_max_amt", length = 30) private String earnMaxAmt;
    @Lob @Column(name = "earn_etc_cn", columnDefinition = "LONGTEXT") private String earnEtcCn;
    @Column(name = "job_cd", length = 500) private String jobCd;
    @Lob @Column(name = "plcy_major_cd", columnDefinition = "LONGTEXT") private String plcyMajorCd;
    @Column(name = "school_cd", length = 500) private String schoolCd;
    @Column(name = "sbiz_cd", length = 500) private String sbizCd;
    @Lob @Column(name = "add_aply_qlfc_cnd_cn", columnDefinition = "LONGTEXT") private String addAplyQlfcCndCn;
    @Lob @Column(name = "ptcp_prp_trgt_cn", columnDefinition = "LONGTEXT") private String ptcpPrpTrgtCn;

    // 심사/제출/기타
    @Lob @Column(name = "srng_mthd_cn", columnDefinition = "LONGTEXT") private String srngMthdCn;
    @Lob @Column(name = "sbmsn_dcmnt_cn", columnDefinition = "LONGTEXT") private String sbmsnDcmntCn;
    @Lob @Column(name = "etc_mttr_cn", columnDefinition = "LONGTEXT") private String etcMttrCn;
    @Column(name = "ref_url_addr1", length = 1000) private String refUrlAddr1;
    @Column(name = "ref_url_addr2", length = 1000) private String refUrlAddr2;

    // 기타
    @Lob @Column(name = "zip_cd", columnDefinition = "LONGTEXT") private String zipCd;
    @Column(name = "inq_cnt", length = 20)  private String inqCnt;

    @Column(name = "frst_reg_dt", length = 30) private String frstRegDt;
    @Column(name = "last_mdfcn_dt", length = 30) private String lastMdfcnDt;

    /** 신규 엔티티 생성 */
    public static YouthPolicy of(Policy p) {
        YouthPolicy e = new YouthPolicy();
        e.plcyNo = nz(p.getPlcyNo());
        e.apply(p);
        return e;
    }

    /** 기존 엔티티 갱신(허용된 경로) */
    public void apply(Policy p) {
        this.plcyNm = nz(p.getPlcyNm());
        this.plcyKywdNm = nz(p.getPlcyKywdNm());
        this.plcyExplnCn = nz(p.getPlcyExplnCn());

        this.lclsfNm = nz(p.getLclsfNm());
        this.mclsfNm = nz(p.getMclsfNm());

        this.plcySprtCn = nz(p.getPlcySprtCn());
        this.sprtArvlSeqYn = nz(p.getSprtArvlSeqYn());
        this.sprtSclLmtYn = nz(p.getSprtSclLmtYn());
        this.sprtSclCnt = nz(p.getSprtSclCnt());

        this.aplyPrdSeCd = nz(p.getAplyPrdSeCd());
        this.aplyYmd = nz(p.getAplyYmd());
        this.aplyUrlAddr = nz(p.getAplyUrlAddr());
        this.plcyAplyMthdCn = nz(p.getPlcyAplyMthdCn());

        this.sprvsnInstCdNm = nz(p.getSprvsnInstCdNm());
        this.operInstCdNm = nz(p.getOperInstCdNm());

        this.sprtTrgtMinAge = nz(p.getSprtTrgtMinAge());
        this.sprtTrgtMaxAge = nz(p.getSprtTrgtMaxAge());
        this.sprtTrgtAgeLmtYn = nz(p.getSprtTrgtAgeLmtYn());
        this.mrgSttsCd = nz(p.getMrgSttsCd());
        this.earnCndSeCd = nz(p.getEarnCndSeCd());
        this.earnMinAmt = nz(p.getEarnMinAmt());
        this.earnMaxAmt = nz(p.getEarnMaxAmt());
        this.earnEtcCn = nz(p.getEarnEtcCn());
        this.jobCd = nz(p.getJobCd());
        this.plcyMajorCd = nz(p.getPlcyMajorCd());
        this.schoolCd = nz(p.getSchoolCd());
        this.sbizCd = nz(p.getSbizCd());
        this.addAplyQlfcCndCn = nz(p.getAddAplyQlfcCndCn());
        this.ptcpPrpTrgtCn = nz(p.getPtcpPrpTrgtCn());

        this.srngMthdCn = nz(p.getSrngMthdCn());
        this.sbmsnDcmntCn = nz(p.getSbmsnDcmntCn());
        this.etcMttrCn = nz(p.getEtcMttrCn());
        this.refUrlAddr1 = nz(p.getRefUrlAddr1());
        this.refUrlAddr2 = nz(p.getRefUrlAddr2());

        this.zipCd = nz(p.getZipCd());
        this.inqCnt = nz(p.getInqCnt());

        this.frstRegDt = nz(p.getFrstRegDt());
        this.lastMdfcnDt = nz(p.getLastMdfcnDt());
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
