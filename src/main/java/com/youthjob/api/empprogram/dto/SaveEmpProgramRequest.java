package com.youthjob.api.empprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveEmpProgramRequest {
    @NotBlank private String orgNm;
    @NotBlank private String pgmNm;
    private String pgmSubNm;
    private String pgmTarget;

    @Pattern(regexp = "^\\d{8}$", message = "pgmStdt must be YYYYMMDD")
    private String pgmStdt;
    @Pattern(regexp = "^\\d{8}$", message = "pgmEndt must be YYYYMMDD")
    private String pgmEndt;

    private String openTimeClcd;   // "1"/"2"
    private String openTime;       // "09:30"
    private String operationTime;  // "2", "12" 등
    private String openPlcCont;
}
