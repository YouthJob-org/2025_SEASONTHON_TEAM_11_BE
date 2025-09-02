package com.youthjob.api.empprogram.controller;

import com.youthjob.api.empprogram.dto.EmpProgramResponseDto;
import com.youthjob.api.empprogram.dto.SaveEmpProgramRequest;
import com.youthjob.api.empprogram.dto.SavedEmpProgramDto;
import com.youthjob.api.empprogram.service.EmpProgramService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emp-programs")
@Validated
@RequiredArgsConstructor
public class EmpProgramController {

    private final EmpProgramService service;

    @GetMapping(produces = "application/json")
    public ResponseEntity<EmpProgramResponseDto> search(
            @RequestParam(required = false)
            @Pattern(regexp = "^\\d{8}$", message = "pgmStdt must be YYYYMMDD") String pgmStdt,
            @RequestParam(required = false) String topOrgCd,
            @RequestParam(required = false) String orgCd,
            @RequestParam(defaultValue = "1") @Min(1) @Max(1000) Integer startPage,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer display
    ) {
        return ResponseEntity.ok(
                service.search(pgmStdt, topOrgCd, orgCd, startPage, display)
        );
    }

    /** 선택 프로그램 저장 */
    @PostMapping(value = "/saved", consumes = "application/json", produces = "application/json")
    public ResponseEntity<SavedEmpProgramDto> save(
            @Valid @RequestBody SaveEmpProgramRequest req,
            // 프로젝트의 Principal 구조에 맞게 수정 가능:
            // e.g. @AuthenticationPrincipal CustomUser user -> Long memberId = user.getId();
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ResponseEntity.ok(service.save(memberId, req));
    }

    /** 내 저장목록 조회 */
    @GetMapping(value = "/saved", produces = "application/json")
    public ResponseEntity<List<SavedEmpProgramDto>> list(
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ResponseEntity.ok(service.list(memberId));
    }

    /** 저장항목 삭제 */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        service.delete(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
