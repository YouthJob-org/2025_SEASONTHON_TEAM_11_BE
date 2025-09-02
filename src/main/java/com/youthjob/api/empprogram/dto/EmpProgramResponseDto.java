package com.youthjob.api.empprogram.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "empPgmSchdInviteList")
public class EmpProgramResponseDto {

    @JacksonXmlProperty(localName = "total")     private Integer total;
    @JacksonXmlProperty(localName = "startPage") private Integer startPage;
    @JacksonXmlProperty(localName = "display")   private Integer display;

    // “정보가 존재하지 않습니다” 같은 케이스 대비
    @JacksonXmlProperty(localName = "message")   private String message;
    @JacksonXmlProperty(localName = "messageCd") private String messageCd;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "empPgmSchdInvite")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<EmpProgramItemDto> programs;
}
