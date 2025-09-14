package com.youthjob.api.youthpolicy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouthPolicyIngestScheduler {

    private final YouthPolicyService service;

    /** 매일 자정(서울) 전체 동기화: 외부 API 전수 수집 → DB 업서트 */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void ingestNightly() {
        try {
            long cnt = service.ingestAll(true); // 내부는 업서트이므로 safe
            log.info("[Scheduler] YouthPolicy nightly ingest OK. fetched={}", cnt);
        } catch (Exception e) {
            log.error("[Scheduler] YouthPolicy nightly ingest FAILED", e);
        }
    }
}
