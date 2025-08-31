package com.youthjob.api.youthpolicy.domain;

import com.youthjob.api.auth.domain.User;
import com.youthjob.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "saved_policy",
    uniqueConstraints = @UniqueConstraint(name = "uk_saved_policy_user_plcyno", columnNames = {"user_id","plcy_no"}),
    indexes = {
        @Index(name = "idx_saved_policy_user", columnList = "user_id"),
        @Index(name = "idx_saved_policy_plcyno", columnList = "plcy_no")
    }
)
public class SavedPolicy extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 저장한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_saved_policy_user"))
    private User user;

    /** 청년정책 식별자 */
    @Column(name = "plcy_no", nullable = false, length = 30)
    private String plcyNo;

    // ===== 스냅샷(화면 편의용) =====
    @Column(length = 300) private String plcyNm;        // 정책명
    @Column(length = 200) private String plcyKywdNm;    // 키워드
    @Column(length = 50)  private String lclsfNm;       // 대분류
    @Column(length = 50)  private String mclsfNm;       // 중분류
    @Column(length = 50)  private String aplyYmd;       // 신청기간 (예: "20250312 ~ 20250414")
    @Column(length = 500) private String aplyUrlAddr;   // 신청 URL
    @Column(length = 200) private String sprvsnInstCdNm;// 주관기관코드명
    @Column(length = 200) private String operInstCdNm;  // 운영기관코드명

    public SavedPolicy(User user, String plcyNo,
                       String plcyNm, String plcyKywdNm,
                       String lclsfNm, String mclsfNm,
                       String aplyYmd, String aplyUrlAddr,
                       String sprvsnInstCdNm, String operInstCdNm) {
        this.user = user;
        this.plcyNo = plcyNo;
        this.plcyNm = plcyNm;
        this.plcyKywdNm = plcyKywdNm;
        this.lclsfNm = lclsfNm;
        this.mclsfNm = mclsfNm;
        this.aplyYmd = aplyYmd;
        this.aplyUrlAddr = aplyUrlAddr;
        this.sprvsnInstCdNm = sprvsnInstCdNm;
        this.operInstCdNm = operInstCdNm;
    }
}
