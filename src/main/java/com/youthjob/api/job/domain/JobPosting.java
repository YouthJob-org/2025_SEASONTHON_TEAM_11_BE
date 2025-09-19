// src/main/java/com/youthjob/api/job/domain/JobPosting.java
package com.youthjob.api.job.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "job_postings", indexes = {
        @Index(name = "idx_job_deadline", columnList = "deadline"),
        @Index(name = "idx_job_regdate", columnList = "regDate")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class JobPosting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상세 URL 해시(또는 공고번호)를 외부 키로 사용 */
    @Column(nullable = false, unique = true, length = 64)
    private String externalId;

    private String title;
    private String company;
    private String region;      // 필요시 파싱
    private String detailUrl;

    private LocalDate regDate;  // 등록일
    private LocalDate deadline; // 마감일

    private String employmentType;
    private String salary;
    private String contact;
}
