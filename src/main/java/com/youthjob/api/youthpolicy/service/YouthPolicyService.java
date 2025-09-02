package com.youthjob.api.youthpolicy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto.Policy; // <-- 올바른 import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class YouthPolicyService {

    private final YouthPolicyClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public YouthPolicyApiResponseDto searchWithFilter(YouthPolicyApiRequestDto req) {
        YouthPolicyApiResponseDto raw = client.search(req);
        if (raw == null || raw.getResult() == null || raw.getResult().getYouthPolicyList() == null) {
            return raw;
        }

        List<Policy> list = raw.getResult().getYouthPolicyList();

        // ==== 서버단 후처리 필터 ====
        if (notBlank(req.getPlcyKywdNm())) {
            String kw = req.getPlcyKywdNm().trim();
            list = list.stream()
                    .filter(p -> containsAnyToken(p.getPlcyKywdNm(), kw))
                    .collect(Collectors.toList());
        }
        if (notBlank(req.getPlcyNm())) {
            String kw = req.getPlcyNm().trim();
            list = list.stream()
                    .filter(p -> contains(p.getPlcyNm(), kw))
                    .collect(Collectors.toList());
        }
        if (notBlank(req.getPlcyExpInCn())) { // 요청 키 기준 설명 필터(부분일치)
            String kw = req.getPlcyExpInCn().trim();
            list = list.stream()
                    .filter(p -> contains(p.getPlcyExplnCn(), kw))
                    .collect(Collectors.toList());
        }
        if (notBlank(req.getZipCd())) {
            String kw = req.getZipCd().trim();
            list = list.stream()
                    .filter(p -> contains(p.getZipCd(), kw))
                    .collect(Collectors.toList());
        }
        if (notBlank(req.getLclsfNm())) {
            String kw = req.getLclsfNm().trim();
            list = list.stream()
                    .filter(p -> contains(p.getLclsfNm(), kw))
                    .collect(Collectors.toList());
        }
        if (notBlank(req.getMclsfNm())) {
            String kw = req.getMclsfNm().trim();
            list = list.stream()
                    .filter(p -> contains(p.getMclsfNm(), kw))
                    .collect(Collectors.toList());
        }

        // ==== 서버단 페이징(외부 pageSize 무시 대비) ====
        int pageNum  = Optional.ofNullable(req.getPageNum()).orElse(1);
        int pageSize = Optional.ofNullable(req.getPageSize()).orElse(10);
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to   = Math.min(list.size(), from + pageSize);
        List<Policy> pageSlice = from < to ? list.subList(from, to) : List.of();

        // ==== setter 없이 응답 재구성 ====
        Map<String, Object> tree = mapper.convertValue(raw, new TypeReference<>() {});
        Map<String, Object> result = castMap(tree.get("result"));
        Map<String, Object> pagging = castMap(result.get("pagging"));

        result.put("youthPolicyList",
                mapper.convertValue(pageSlice, new TypeReference<List<Map<String,Object>>>(){}));
        pagging.put("totCount", list.size());
        pagging.put("pageNum", pageNum);
        pagging.put("pageSize", pageSize);

        return mapper.convertValue(tree, YouthPolicyApiResponseDto.class);
    }

    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }
    private static boolean contains(String src, String kw){ return src != null && src.contains(kw); }

    /** "교육지원,보조금" 같은 콤마 구분 키워드에서 부분일치 허용 */
    private static boolean containsAnyToken(String src, String kw){
        if (src == null || kw == null || kw.isBlank()) return false;
        return Stream.of(src.split("[,，]"))
                .map(String::trim)
                .anyMatch(t -> t.contains(kw));
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> castMap(Object o){
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<>();
    }
}
