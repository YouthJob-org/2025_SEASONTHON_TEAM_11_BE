package com.youthjob.api.job.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class Enriched {
    private String title;
    private String company;
    private String region;
    private String employmentType;
    private String salary;
    private String contact;
    private LocalDate regDate;
    private LocalDate deadline;
}
