package com.youthjob.api.empprogram.controller;

import com.youthjob.api.empprogram.dto.EmpProgramResponseDto;
import com.youthjob.api.empprogram.dto.SaveEmpProgramRequest;
import com.youthjob.api.empprogram.dto.SavedEmpProgramDto;
import com.youthjob.api.empprogram.service.EmpProgramService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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


@Tag(name = "EMP", description = "취업역량 강화 프로그램 검색,조회,저장 관련 API입니다.")
@RestController
@RequestMapping("/api/v1/emp-programs")
@Validated
@RequiredArgsConstructor
public class EmpProgramController {

    private final EmpProgramService service;

    @Operation(
            summary = "고용훈련 프로그램 검색",
            description = "pgmStdt, 기관코드 등으로 고용훈련 프로그램을 검색"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode="401", description="인증 필요",
                    content = @Content(mediaType="application/json",
                            schema = @Schema(implementation = com.youthjob.common.response.ApiResponse.class),
                            examples = @ExampleObject(
                                    name="unauthorized",
                                    value="""
                {"status":401,"success":false,"message":"인증 필요","data":null}
                """
                            )
                    )
            )
    })
    @GetMapping(produces = "application/json")
    public ResponseEntity<ApiResponse<EmpProgramResponseDto>> search(
            @RequestParam(required = false)
            @Pattern(regexp = "^\\d{8}$", message = "pgmStdt must be YYYYMMDD") String pgmStdt,
            @RequestParam(required = false) String topOrgCd,
            @RequestParam(required = false) String orgCd,
            @RequestParam(defaultValue = "1") @Min(1) @Max(1000) Integer startPage,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer display
    ) {
        return ApiResponse.success(
                SuccessStatus.EMP_PROGRAM_SEARCH_SUCCESS,
                service.search(pgmStdt, topOrgCd, orgCd, startPage, display)
        );
    }

    @Operation(summary = "고용훈련 프로그램 저장", description = "선택한 프로그램을 내 저장목록에 추가")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode="401", description="인증 필요",
                    content = @Content(mediaType="application/json",
                            schema = @Schema(implementation = com.youthjob.common.response.ApiResponse.class),
                            examples = @ExampleObject(
                                    name="unauthorized",
                                    value="""
                {"status":401,"success":false,"message":"인증 필요","data":null}
                """
                            )
                    )
            )
    })
    @PostMapping(value = "/saved", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<SavedEmpProgramDto>> save(
            @Valid @RequestBody SaveEmpProgramRequest req,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ApiResponse.success(
                SuccessStatus.EMP_PROGRAM_SAVE_SUCCESS,
                service.save(memberId, req)
        );
    }


    @Operation(summary = "저장목록 조회", description = "내가 저장한 고용훈련 프로그램 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode="401", description="인증 필요",
                    content = @Content(mediaType="application/json",
                            schema = @Schema(implementation = com.youthjob.common.response.ApiResponse.class),
                            examples = @ExampleObject(
                                    name="unauthorized",
                                    value="""
                {"status":401,"success":false,"message":"인증 필요","data":null}
                """
                            )
                    )
            )
    })
    @GetMapping(value = "/saved", produces = "application/json")
    public ResponseEntity<ApiResponse<List<SavedEmpProgramDto>>> list(
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ApiResponse.success(
                SuccessStatus.EMP_PROGRAM_SAVED_LIST_SUCCESS,
                service.list(memberId)
        );
    }


    @Operation(summary = "저장항목 삭제", description = "저장 목록에서 항목 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode="401", description="인증 필요",
                    content = @Content(mediaType="application/json",
                            schema = @Schema(implementation = com.youthjob.common.response.ApiResponse.class),
                            examples = @ExampleObject(
                                    name="unauthorized",
                                    value="""
                {"status":401,"success":false,"message":"인증 필요","data":null}
                """
                            )
                    )
            ),
    })
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        service.delete(memberId, id);
        return ApiResponse.successOnly(SuccessStatus.EMP_PROGRAM_DELETE_SUCCESS);
    }

    @Operation(summary = "저장항목 토글", description = "저장 목록에서 항목 삭제 OR 저장")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode="401", description="인증 필요",
                    content = @Content(mediaType="application/json",
                            schema = @Schema(implementation = com.youthjob.common.response.ApiResponse.class),
                            examples = @ExampleObject(
                                    name="unauthorized",
                                    value="""
                {"status":401,"success":false,"message":"인증 필요","data":null}
                """
                            )
                    )
            ),
    })
    @PostMapping("/saved/toggle")
    public ResponseEntity<ApiResponse<SavedEmpProgramDto>> toggle(
            @Valid @RequestBody SaveEmpProgramRequest req,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ApiResponse.success(
                SuccessStatus.EMP_PROGRAM_SAVED_TOGGLE_SUCCESS,
                service.toggle(memberId, req)
        );
    }


}
