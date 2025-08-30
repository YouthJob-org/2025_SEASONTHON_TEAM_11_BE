package com.example.api.hrd.service;

import com.example.api.hrd.client.HrdApiClient;
import com.example.api.hrd.dto.HrdCourseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrdSearchService {
    private final HrdApiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<HrdCourseDto> search(String startDt, String endDt, int page, int size, String area1, String ncs1, String sort, String sortCol) {
        String json = client.search(startDt, endDt, page, size, area1, ncs1, sort, sortCol);
        try {
            JsonNode root = mapper.readTree(json);
            // 질문에 준 예시 구조에 맞춰 파싱
            JsonNode srchList = root.get("srchList");
            if (srchList == null) { // HRDNet 감싸는 경우 대비
                JsonNode hrd = root.get("HRDNet");
                if (hrd != null) {
                    JsonNode inner = hrd.get("srchList");
                    srchList = inner != null ? inner.get("scn_list") : null;
                }
            } else {
                srchList = srchList;
            }

            List<HrdCourseDto> result = new ArrayList<>();
            JsonNode array = srchList.isArray() ? srchList : root.get("srchList"); // 질문 예시 형태
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    result.add(HrdCourseDto.builder()
                            .title(n.path("title").asText(null))
                            .subTitle(n.path("subTitle").asText(null))
                            .address(n.path("address").asText(null))
                            .telNo(n.path("telNo").asText(null))
                            .traStartDate(n.path("traStartDate").asText(null))
                            .traEndDate(n.path("traEndDate").asText(null))
                            .trainTarget(n.path("trainTarget").asText(null))
                            .trainTargetCd(n.path("trainTargetCd").asText(null))
                            .ncsCd(n.path("ncsCd").asText(null))
                            .trprId(n.path("trprId").asText(null))
                            .trprDegr(n.path("trprDegr").asText(null))
                            .courseMan(n.path("courseMan").asText(null))
                            .realMan(n.path("realMan").asText(null))
                            .yardMan(n.path("yardMan").asText(null))
                            .titleLink(n.path("titleLink").asText(null))
                            .subTitleLink(n.path("subTitleLink").asText(null))
                            .build());
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("HRD 응답 파싱 실패", e);
        }
    }
}
