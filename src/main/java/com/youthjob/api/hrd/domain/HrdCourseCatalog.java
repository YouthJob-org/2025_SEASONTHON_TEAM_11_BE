package com.youthjob.api.hrd.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.Instant;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "hrd_course_catalog",
        uniqueConstraints = @UniqueConstraint(name="uk_hrd_catalog_course", columnNames = {"trprId","trprDegr"}),
        indexes = {
                @Index(name="idx_hrd_catalog_dates", columnList = "traStartDate,traEndDate"),
                @Index(name="idx_hrd_catalog_ncs", columnList = "ncsCd"),
                @Index(name="idx_hrd_catalog_area", columnList = "area1"),
                @Index(name="idx_hrd_catalog_torg", columnList = "torgId")
        })
public class HrdCourseCatalog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 복합 식별자 성격
    @Column(nullable=false, length=50) private String trprId;
    @Column(nullable=false, length=20) private String trprDegr;
    @Column(length=50)                private String torgId;

    // 검색/정렬에 쓰는 핵심 필드
    @Column(nullable=false) private LocalDate traStartDate;
    @Column(nullable=false) private LocalDate traEndDate;

    // 필터용(선택)
    @Column(length=10) private String area1;   // srchTraArea1 (예: 11, 26 등)
    @Column(length=10) private String ncsCd;   // 대분류 코드만 저장해도 OK

    // 화면 메타
    @Column(length=300) private String title;
    @Column(length=300) private String subTitle;
    @Column(length=300) private String address;
    @Column(length=50)  private String telNo;

    private String trainTarget;
    private String trainTargetCd;

    private String courseMan;
    private String realMan;
    private String yardMan;

    @Column(length=1000) private String titleLink;
    @Column(length=1000) private String subTitleLink;

    // 동기화 시각
    private Instant syncedAt;
}
