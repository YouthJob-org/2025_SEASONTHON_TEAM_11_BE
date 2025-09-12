package com.youthjob.api.empprogram.controller;

import com.youthjob.api.empprogram.service.EmpProgramCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/emp-programs")
@RequiredArgsConstructor
public class EmpProgramIngestController {

    private final EmpProgramCatalogService catalog;

    /** 오늘(한국시간) 기준 전체 수집 */
    @PostMapping("/harvest/today")
    public ResponseEntity<Map<String, Object>> harvestToday() {
        String day = EmpProgramCatalogService.todayYyyymmdd();
        int inserted = catalog.harvestAllForDay(day);
        return ResponseEntity.ok(Map.of("day", day, "inserted", inserted));
    }

    /** 특정 일자(YYYYMMDD) 전체 수집 */
    @PostMapping("/harvest/{yyyymmdd}")
    public ResponseEntity<Map<String, Object>> harvestFor(@PathVariable String yyyymmdd) {
        int inserted = catalog.harvestAllForDay(yyyymmdd);
        return ResponseEntity.ok(Map.of("day", yyyymmdd, "inserted", inserted));
    }

    /** 오늘부터 6개월치 롤링 수집 */
    @PostMapping("/harvest/rolling-6m")
    public ResponseEntity<Map<String, Object>> harvestRolling() {
        String today = EmpProgramCatalogService.todayYyyymmdd();
        int inserted = catalog.harvestRollingSixMonths(today);
        return ResponseEntity.ok(Map.of("from", today, "months", 6, "inserted", inserted));
    }

    /** 지난 일정(pgmEndt < today) 일괄 삭제 */
    @DeleteMapping("/purge/past")
    public ResponseEntity<Map<String, Object>> purgePast() {
        String today = EmpProgramCatalogService.todayYyyymmdd();
        int deleted = catalog.purgePast(today);
        return ResponseEntity.ok(Map.of("today", today, "deleted", deleted));
    }
}
