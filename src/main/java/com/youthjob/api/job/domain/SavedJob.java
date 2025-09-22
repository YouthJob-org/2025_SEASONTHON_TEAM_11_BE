package com.youthjob.api.job.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_jobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_job_user_ext", columnNames = {"user_id","job_external_id"})
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedJob {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인한 사용자 식별자 */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** JobPosting.externalId 참조 */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "job_external_id", referencedColumnName = "externalId", nullable = false)
    private JobPosting job;
}
