package com.youthjob.api.hrd.domain;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.*;

@Converter(autoApply = false)
public class StatsJsonConverter implements AttributeConverter<List<com.youthjob.api.hrd.dto.HrdCourseStatDto>, String> {
    private static final ObjectMapper M = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule())
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String convertToDatabaseColumn(List<com.youthjob.api.hrd.dto.HrdCourseStatDto> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) return "[]";
            return M.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("stats->json fail", e);
        }
    }

    @Override
    public List<com.youthjob.api.hrd.dto.HrdCourseStatDto> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return Collections.emptyList();
            JavaType type = M.getTypeFactory()
                    .constructCollectionType(List.class, com.youthjob.api.hrd.dto.HrdCourseStatDto.class);
            return M.readValue(dbData, type);
        } catch (Exception e) {
            // 실패 시에도 전체 저장이 막히지 않게 빈 리스트로 대체하거나 메시지를 명확히 남기세요.
            throw new IllegalArgumentException("json->stats fail: " + (dbData.length()>200?dbData.substring(0,200)+"...":dbData), e);
        }
    }
}