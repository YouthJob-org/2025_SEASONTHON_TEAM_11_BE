package com.youthjob.api.job.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingSaveRequest {
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
