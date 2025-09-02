package com.youthjob.api.empprogram.domain;

import com.youthjob.common.entity.BaseTimeEntity; // 프로젝트에 이미 있음
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "saved_emp_program",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_extkey", columnNames = {"member_id", "ext_key"})
)
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class SavedEmpProgram extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 외부데이터의 식별 키(중복 저장 방지) */
    @Column(name = "ext_key", nullable = false, length = 512)
    private String extKey;

    @Column(length = 100) private String orgNm;
    @Column(length = 200) private String pgmNm;
    @Column(length = 500) private String pgmSubNm;
    @Column(length = 100) private String pgmTarget;
    @Column(length = 8)   private String pgmStdt;
    @Column(length = 8)   private String pgmEndt;
    @Column(length = 2)   private String openTimeClcd;
    @Column(length = 5)   private String openTime;        // HH:MM (12h 표기 그대로 저장)
    @Column(length = 20)  private String operationTime;
    @Column(length = 300) private String openPlcCont;
}
