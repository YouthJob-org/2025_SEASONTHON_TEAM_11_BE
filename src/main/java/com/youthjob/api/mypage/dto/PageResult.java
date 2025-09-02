package com.youthjob.api.mypage.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record PageResult<T>(
        int page, int size, long totalElements, int totalPages, List<T> items
) {}
