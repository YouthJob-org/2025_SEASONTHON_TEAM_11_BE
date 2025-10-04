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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 64)
    private String externalId;

    private String title;
    private String company;
    private String region;
    private String detailUrl;

    private LocalDate regDate;
    private LocalDate deadline;

    private String employmentType;
    private String salary;
}
