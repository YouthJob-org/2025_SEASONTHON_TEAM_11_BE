package com.youthjob.api.hrd.dto;

import com.youthjob.api.hrd.domain.SavedCourse;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Builder
public record SavedCourseDto(
        Long id,
        String trprId, String trprDegr,
        String torgId,
        LocalDateTime createdAt
) {
    public static SavedCourseDto from(SavedCourse s) {
        return SavedCourseDto.builder()
                .id(s.getId())
                .trprId(s.getTrprId())
                .trprDegr(s.getTrprDegr())
                .torgId(s.getTorgId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}

