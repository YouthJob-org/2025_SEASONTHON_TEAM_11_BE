package com.youthjob.api.hrd.domain;

import com.youthjob.api.auth.domain.User;
import com.youthjob.common.entity.BaseTimeEntity;
import com.youthjob.common.exception.BaseException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@Builder
@AllArgsConstructor @NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "saved_course",
        indexes = {
                @Index(name = "idx_saved_course_user", columnList = "user_id"),
                @Index(name = "idx_saved_course_trpr", columnList = "trprId,trprDegr")
        },
        uniqueConstraints = {
                // 유저별 같은 과정/회차 중복 방지
                @UniqueConstraint(name = "uk_saved_course_user_trpr", columnNames = {"user_id","trprId","trprDegr"})
        }
)
public class SavedCourse extends BaseTimeEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_saved_course_user"))
    private User user;

    // 과정 식별
    @Column(nullable = false, length = 50)
    private String trprId;

    @Column(nullable = false, length = 20)
    private String trprDegr;

    // 화면 표시용 메타
    private String title;
    private String subTitle;
    private String address;
    private String telNo;

    private String traStartDate;
    private String traEndDate;

    private String trainTarget;
    private String trainTargetCd;

    private String ncsCd;

    private String courseMan;
    private String realMan;
    private String yardMan;

    @Column(length = 1000)
    private String titleLink;

    @Column(length = 1000)
    private String subTitleLink;

}
