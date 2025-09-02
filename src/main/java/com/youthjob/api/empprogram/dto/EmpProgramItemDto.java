package com.youthjob.api.empprogram.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpProgramItemDto {

    @JacksonXmlProperty(localName = "orgNm")        private String orgNm;
    @JacksonXmlProperty(localName = "pgmNm")        private String pgmNm;
    @JacksonXmlProperty(localName = "pgmSubNm")     private String pgmSubNm;
    @JacksonXmlProperty(localName = "pgmTarget")    private String pgmTarget;
    @JacksonXmlProperty(localName = "pgmStdt")      private String pgmStdt;
    @JacksonXmlProperty(localName = "pgmEndt")      private String pgmEndt;
    @JacksonXmlProperty(localName = "openTimeClcd") private String openTimeClcd;
    @JacksonXmlProperty(localName = "openTime")     private String openTime;
    @JacksonXmlProperty(localName = "operationTime")private String operationTime;
    @JacksonXmlProperty(localName = "openPlcCont")  private String openPlcCont;
}
