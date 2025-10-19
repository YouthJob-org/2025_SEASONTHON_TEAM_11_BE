package com.youthjob.api.hrd.domain;

import com.youthjob.api.auth.domain.User;
import com.youthjob.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name="saved_course",
        uniqueConstraints = @UniqueConstraint(name="uk_saved_course_user_trpr", columnNames={"user_id","trprId","trprDegr"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedCourse extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", foreignKey=@ForeignKey(name="fk_saved_course_user"))
    private User user;

    @Column(nullable=false, length=50)
    private String trprId;
    @Column(nullable=false, length=20)
    private String trprDegr;

    @Column(length=50)
    private String torgId;
}

