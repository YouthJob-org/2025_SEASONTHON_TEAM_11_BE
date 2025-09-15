package com.youthjob.api.hrd.controller;

import com.youthjob.api.hrd.service.HrdCatalogSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/hrd")
class HrdInternalSyncController {
    private final HrdCatalogSyncService sync;

    @PostMapping("/harvest")
    public java.util.Map<String,Object> harvest(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) String area1,
            @RequestParam(required = false) String ncs1,
            @RequestParam(defaultValue = "true") boolean purgeEndedFirst
    ) {
        int deleted = purgeEndedFirst ? sync.purgeEnded() : 0;
        int upserts = sync.harvestMonthsAhead(months, area1, ncs1);
        return java.util.Map.of("deleted", deleted, "upserts", upserts, "months", months);
    }

    @PostMapping("/harvest-full")
    public Map<String,Object> harvestFull(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) String area1,
            @RequestParam(required = false) String ncs1,
            @RequestParam(defaultValue = "500") int pageSize,
            @RequestParam(defaultValue = "0") int maxItems, // 0=무제한
            @RequestParam(defaultValue = "8") int concurrency // 기본 8
    ) {
        int processed = sync.harvestFullMonthsAheadParallel(months, area1, ncs1, pageSize, maxItems, concurrency);
        return Map.of("months", months, "processed", processed);
    }
}
