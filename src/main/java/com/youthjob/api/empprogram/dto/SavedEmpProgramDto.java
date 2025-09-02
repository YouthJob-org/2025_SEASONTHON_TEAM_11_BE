package com.youthjob.api.empprogram.dto;

import com.youthjob.api.empprogram.domain.SavedEmpProgram;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedEmpProgramDto {
    private Long id;
    private String orgNm;
    private String pgmNm;
    private String pgmSubNm;
    private String pgmTarget;
    private String pgmStdt;
    private String pgmEndt;
    private String openTimeClcd;
    private String openTime;
    private String operationTime;
    private String openPlcCont;
    private String createdAt;  // ISO 문자열

    public static SavedEmpProgramDto from(SavedEmpProgram e) {
        return SavedEmpProgramDto.builder()
                .id(e.getId())
                .orgNm(e.getOrgNm())
                .pgmNm(e.getPgmNm())
                .pgmSubNm(e.getPgmSubNm())
                .pgmTarget(e.getPgmTarget())
                .pgmStdt(e.getPgmStdt())
                .pgmEndt(e.getPgmEndt())
                .openTimeClcd(e.getOpenTimeClcd())
                .openTime(e.getOpenTime())
                .operationTime(e.getOperationTime())
                .openPlcCont(e.getOpenPlcCont())
                .createdAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString())
                .build();
    }
}
