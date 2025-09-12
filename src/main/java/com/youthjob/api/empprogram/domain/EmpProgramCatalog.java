package com.youthjob.api.empprogram.domain;

import com.youthjob.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "emp_program_catalog",
        indexes = {
                @Index(name = "ix_pgmStdt", columnList = "pgmStdt"),
                @Index(name = "ix_pgmEndt", columnList = "pgmEndt")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_ext_key", columnNames = "ext_key")
)
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class EmpProgramCatalog extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "ext_key", nullable = false, length = 512)
    private String extKey;

    @Column(length = 100) private String orgNm;
    @Column(length = 200) private String pgmNm;
    @Column(length = 500) private String pgmSubNm;
    @Column(length = 100) private String pgmTarget;
    @Column(length = 8)   private String pgmStdt;  // YYYYMMDD
    @Column(length = 8)   private String pgmEndt;  // YYYYMMDD
    @Column(length = 2)   private String openTimeClcd;
    @Column(length = 5)   private String openTime;        // HH:MM (12h 표기 그대로 저장)
    @Column(length = 20)  private String operationTime;
    @Column(length = 300) private String openPlcCont;

    // EmpProgramCatalog.java (추가 필드만)
    @Column(length = 10) private String topOrgCd;  // 지방청 코드(1 depth)
    @Column(length = 10) private String orgCd;     // 센터 코드(2 depth)

}
