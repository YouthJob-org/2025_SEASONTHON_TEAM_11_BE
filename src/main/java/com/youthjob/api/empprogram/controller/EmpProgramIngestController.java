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

    /** 오늘부터 6개월치 롤링 수집, 지난 교육 삭제 */
    @PostMapping("/harvest/rolling-6m")
    public ResponseEntity<Map<String, Object>> harvestRolling() {
        String today = EmpProgramCatalogService.todayYyyymmdd();
        int deleted = catalog.purgePast(today);
        int inserted = catalog.harvestRollingSixMonths(today);
        return ResponseEntity.ok(Map.of("from", today, "months", 6, "inserted", inserted,"delete",deleted));
    }

}
