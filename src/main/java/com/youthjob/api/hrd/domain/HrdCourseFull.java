package com.youthjob.api.hrd.domain;

import com.youthjob.api.hrd.dto.HrdCourseDetailDto;
import com.youthjob.api.hrd.dto.HrdCourseStatDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "hrd_course_full",
        uniqueConstraints = @UniqueConstraint(name="uk_hrd_course_full", columnNames = {"trprId","trprDegr","torgId"}),
        indexes = {
                @Index(name="idx_full_trpr", columnList = "trprId,trprDegr"),
                @Index(name="idx_full_torg", columnList = "torgId")
        })
public class HrdCourseFull {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 식별
    @Column(nullable=false, length=50) private String trprId;
    @Column(nullable=false, length=20) private String trprDegr;
    @Column(nullable=false, length=50) private String torgId;


    private String trprNm;
    private String ncsCd;
    private String ncsNm;
    private String traingMthCd;
    private String trtm;
    private String trDcnt;
    private String perTrco;
    private String instPerTrco;

    private String inoNm;
    private String addr1;
    private String addr2;
    private String zipCd;
    private String hpAddr;
    private String trprChap;
    private String trprChapTel;
    private String trprChapEmail;

    private String filePath;
    private String pFileName;
    private String torgParGrad;


    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = StatsJsonConverter.class)
    private List<HrdCourseStatDto> stats;


    private Instant savedAt;


    public void applyDetail(HrdCourseDetailDto d) {
        this.trprNm = d.trprNm();
        this.ncsCd  = d.ncsCd();
        this.ncsNm  = d.ncsNm();
        this.traingMthCd = d.traingMthCd();
        this.trtm   = d.trtm();
        this.trDcnt = d.trDcnt();
        this.perTrco = d.perTrco();
        this.instPerTrco = d.instPerTrco();

        this.inoNm = d.inoNm();
        this.addr1 = d.addr1();
        this.addr2 = d.addr2();
        this.zipCd = d.zipCd();
        this.hpAddr = d.hpAddr();
        this.trprChap = d.trprChap();
        this.trprChapTel = d.trprChapTel();
        this.trprChapEmail = d.trprChapEmail();

        this.filePath = d.filePath();
        this.pFileName = d.pFileName();
        this.torgParGrad = d.torgParGrad();
    }
}
