package com.youthjob.api.youthpolicy.controller;

import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policies")
public class YouthPolicyController {

    private final YouthPolicyClient client;

    // 예: /api/v1/youth-policies?plcyKywdNm=보조금&pageNum=1&pageSize=10
    @GetMapping
    public YouthPolicyApiResponseDto list(@Valid @ModelAttribute YouthPolicyApiRequestDto req) {
        return client.search(req);
    }
}
