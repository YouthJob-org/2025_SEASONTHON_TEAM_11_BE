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

    public List<HrdCourseDto> search(String startDt, String endDt, int page, int size,
                                     String area1, String ncs1, String sort, String sortCol) {
        String json = client.search(startDt, endDt, page, size, area1, ncs1, sort, sortCol);
        try {
            JsonNode root = mapper.readTree(json);

            // (옵션) 에러 응답 선감지
            if (root.has("resultCode") && !"0000".equals(root.path("resultCode").asText())) {
                throw new IllegalStateException("HRD API Error: " + root.path("resultMsg").asText());
            }

            // 정석 경로: HRDNet -> srchList -> scn_list
            JsonNode list = root.path("HRDNet").path("srchList").path("scn_list");

            // 방어: 변형 응답 (배열 srchList 혹은 객체 내 list/scn_list)
            if (!list.isArray()) {
                if (root.has("srchList")) {
                    JsonNode s = root.get("srchList");
                    if (s.isArray()) list = s;
                    else if (s.has("scn_list")) list = s.get("scn_list");
                    else if (s.has("list")) list = s.get("list");
                }
            }

            if (!list.isArray()) {
                // 목록이 없으면 원문 일부를 포함해 바로 원인 파악
                throw new IllegalStateException("목록(scn_list)을 찾지 못했습니다. 응답 일부: "
                        + json.substring(0, Math.min(json.length(), 500)));
            }

            List<HrdCourseDto> result = new ArrayList<>();
            for (JsonNode n : list) {
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
            return result;

        } catch (Exception e) {
            throw new RuntimeException("HRD 응답 파싱 실패", e);
        }
    }
}
