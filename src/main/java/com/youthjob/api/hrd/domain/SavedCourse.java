package com.youthjob.api.hrd.domain;

import com.youthjob.api.auth.domain.User;
import com.youthjob.common.entity.BaseTimeEntity;
import com.youthjob.common.exception.BaseException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "saved_course",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_course_user_trpr_degr",
                        columnNames = {"user_id", "trpr_id", "trpr_degr"})
        },
        indexes = {
                @Index(name = "idx_saved_course_user", columnList = "user_id"),
                @Index(name = "idx_saved_course_trpr", columnList = "trpr_id,trpr_degr")
        }
)
public class SavedCourse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 저장한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_saved_course_user"))
    private User user;

    /** HRD 식별자 */
    @Column(name = "trpr_id", nullable = false, length = 50)
    private String trprId;

    @Column(name = "trpr_degr", nullable = false, length = 20)
    private String trprDegr;

    // 스냅샷(변동 가능성 있어도 조회 편의 위해 일부 저장)
    @Column(length = 300) private String title;
    @Column(length = 300) private String subTitle;
    @Column(length = 200) private String address;
    @Column(length = 50)  private String telNo;
    @Column(length = 10)  private String traStartDate;
    @Column(length = 10)  private String traEndDate;
    @Column(length = 60)  private String trainTarget;
    @Column(length = 20)  private String trainTargetCd;
    @Column(length = 20)  private String ncsCd;
    @Column(length = 20)  private String courseMan;
    @Column(length = 20)  private String realMan;
    @Column(length = 20)  private String yardMan;
    @Column(length = 500) private String titleLink;
    @Column(length = 500) private String subTitleLink;

    @Builder
    private SavedCourse(User user, String trprId, String trprDegr,
                        String title, String subTitle, String address, String telNo,
                        String traStartDate, String traEndDate, String trainTarget, String trainTargetCd,
                        String ncsCd, String courseMan, String realMan, String yardMan,
                        String titleLink, String subTitleLink) {
        this.user = user;
        this.trprId = trprId;
        this.trprDegr = trprDegr;
        this.title = title;
        this.subTitle = subTitle;
        this.address = address;
        this.telNo = telNo;
        this.traStartDate = traStartDate;
        this.traEndDate = traEndDate;
        this.trainTarget = trainTarget;
        this.trainTargetCd = trainTargetCd;
        this.ncsCd = ncsCd;
        this.courseMan = courseMan;
        this.realMan = realMan;
        this.yardMan = yardMan;
        this.titleLink = titleLink;
        this.subTitleLink = subTitleLink;
    }
}
