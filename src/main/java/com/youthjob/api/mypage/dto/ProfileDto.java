package com.youthjob.api.mypage.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ProfileDto(
        String displayName,   // name 있으면 name, 없으면 이메일 local-part
        String email,
        LocalDateTime joinedAt,
        String name           // 저장된 이름(수정 화면 초기값)
) {}
