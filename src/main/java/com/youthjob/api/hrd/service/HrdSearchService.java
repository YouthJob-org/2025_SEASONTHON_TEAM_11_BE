package com.youthjob.api.hrd.service;

import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.api.hrd.client.HrdApiClient;
import com.youthjob.api.hrd.domain.SavedCourse;
import com.youthjob.api.hrd.dto.HrdCourseDetailDto;
import com.youthjob.api.hrd.dto.HrdCourseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.api.hrd.dto.SaveCourseRequest;
import com.youthjob.api.hrd.dto.SavedCourseDto;
import com.youthjob.api.hrd.repository.SavedCourseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrdSearchService {
    private final HrdApiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final SavedCourseRepository savedCourseRepository;
    private final UserRepository userRepository;

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
                String titleLink = n.path("titleLink").asText(null);
                String subTitleLink = n.path("subTitleLink").asText(null);

                // torgId 추출 (우선순위: 명시 필드들 → 링크 쿼리)
                String torgId = null;
                if (n.hasNonNull("torgId")) {
                    torgId = n.get("torgId").asText();
                } else if (n.hasNonNull("instIno")) {
                    torgId = n.get("instIno").asText();
                } else if (n.hasNonNull("cstmrId")) {
                    torgId = n.get("cstmrId").asText();
                } else if (n.hasNonNull("trainstCstmrId")) {
                    torgId = n.get("trainstCstmrId").asText();
                } else {
                    // 링크 쿼리에서 키 후보들을 순서대로 시도
                    torgId = firstNonNull(
                            extractQuery(titleLink, "trainstCstmrId"),
                            extractQuery(titleLink, "srchTorgId"),
                            extractQuery(titleLink, "cstmrId"),
                            extractQuery(subTitleLink, "trainstCstmrId"),
                            extractQuery(subTitleLink, "srchTorgId"),
                            extractQuery(subTitleLink, "cstmrId")
                    );
                }

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
                        .torgId(torgId)
                        .build());
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException("HRD 응답 파싱 실패", e);
        }
    }

    /** 현재 로그인 사용자 조회 (username = email 가정) */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + auth.getName()));
    }


    /** 저장 */
    @Transactional
    public SavedCourseDto saveCourse(SaveCourseRequest req) {
        User me = getCurrentUser();

        // 중복 방지
        if (savedCourseRepository.existsByUserAndTrprIdAndTrprDegr(me, req.trprId(), req.trprDegr())) {
            // 이미 있으면 그대로 반환
            return savedCourseRepository.findAllByUserOrderByCreatedAtDesc(me).stream()
                    .filter(c -> c.getTrprId().equals(req.trprId()) && c.getTrprDegr().equals(req.trprDegr()))
                    .findFirst()
                    .map(SavedCourseDto::from)
                    .orElseThrow(() -> new IllegalStateException("이미 저장된 항목을 찾을 수 없습니다."));
        }

        SavedCourse saved = SavedCourse.builder()
                .user(me)
                .trprId(req.trprId())
                .trprDegr(req.trprDegr())
                .title(req.title())
                .subTitle(req.subTitle())
                .address(req.address())
                .telNo(req.telNo())
                .traStartDate(req.traStartDate())
                .traEndDate(req.traEndDate())
                .trainTarget(req.trainTarget())
                .trainTargetCd(req.trainTargetCd())
                .ncsCd(req.ncsCd())
                .courseMan(req.courseMan())
                .realMan(req.realMan())
                .yardMan(req.yardMan())
                .titleLink(req.titleLink())
                .subTitleLink(req.subTitleLink())
                .build();

        return SavedCourseDto.from(savedCourseRepository.save(saved));
    }

    /** 내 저장 목록 */
    public List<SavedCourseDto> listSaved() {
        User me = getCurrentUser();
        return savedCourseRepository.findAllByUserOrderByCreatedAtDesc(me)
                .stream().map(SavedCourseDto::from).toList();
    }

    /** 삭제 */
    @Transactional
    public void deleteSaved(Long id) {
        User me = getCurrentUser();
        var target = savedCourseRepository.findByIdAndUser(id, me)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 항목을 찾을 수 없습니다."));
        savedCourseRepository.delete(target);
    }

    /** (옵션) 토글: 없으면 저장, 있으면 삭제하고 null 반환 */
    @Transactional
    public SavedCourseDto toggleSaved(SaveCourseRequest req) {
        User me = getCurrentUser();
        var exists = savedCourseRepository.existsByUserAndTrprIdAndTrprDegr(me, req.trprId(), req.trprDegr());
        if (exists) {
            var target = savedCourseRepository.findAllByUserOrderByCreatedAtDesc(me).stream()
                    .filter(c -> c.getTrprId().equals(req.trprId()) && c.getTrprDegr().equals(req.trprDegr()))
                    .findFirst()
                    .orElseThrow();
            savedCourseRepository.delete(target);
            return null; // 프런트에서 null이면 "해제됨"으로 처리
        }
        return saveCourse(req);
    }

    public HrdCourseDetailDto getDetail(String trprId, String trprDegr, String torgId) {
        String json = client.getDetail(trprId, trprDegr, torgId);
        try {
            // (임시) 응답 구조 확인용 로그 - 개발 중에만
            // log.debug("HRD detail raw: {}", json.length() > 800 ? json.substring(0, 800) + "..." : json);

            JsonNode root = mapper.readTree(json);

            // 1) HRDNet 래퍼 유무 파악 (없으면 최상위 사용)
            JsonNode hr = root.has("HRDNet") ? root.get("HRDNet") : root;

            // 2) 컨테이너 노드도 대/소문자 모두 시도
            JsonNode baseWrap   = node(hr, "inst_base_info",   "INST_BASE_INFO");
            JsonNode detailWrap = node(hr, "inst_detail_info", "INST_DETAIL_INFO");

            // 배열로 올 수도 있으니 첫 요소를 사용
            JsonNode base   = firstObj(baseWrap);
            JsonNode detail = firstObj(detailWrap);

            // 3) 시설/장비 리스트도 컨테이너/리스트 키 대/소문자 모두 시도 + 배열/단건 모두 처리
            List<HrdCourseDetailDto.Facility> facilities = new ArrayList<>();
            JsonNode facWrap  = node(hr, "inst_facility_info",      "INST_FACILITY_INFO");
            JsonNode facList  = node(facWrap, "inst_facility_info_list", "INST_FACILITY_INFO_LIST");
            for (JsonNode n : asIterable(facList)) facilities.add(mapFacility(n));

            List<HrdCourseDetailDto.Equipment> equipments = new ArrayList<>();
            JsonNode eqpWrap  = node(hr, "inst_eqnm_info",      "INST_EQNM_INFO");
            JsonNode eqpList  = node(eqpWrap, "inst_eqnm_info_list", "INST_EQNM_INFO_LIST");
            for (JsonNode n : asIterable(eqpList)) equipments.add(mapEquipment(n));

            return HrdCourseDetailDto.builder()
                    .trprId(      firstNonNull(val(detail,"trprId","TRPR_ID"), trprId))
                    .trprDegr(    firstNonNull(val(detail,"trprDegr","TRPR_DEGR"), trprDegr))
                    .trprNm(      val(detail,"trprNm","TRPR_NM"))
                    .ncsCd(       val(base,  "ncsCd","NCS_CD"))
                    .ncsNm(       val(base,  "ncsNm","NCS_NM"))
                    .traingMthCd( val(base,  "traingMthCd","TRAING_MTH_CD"))
                    .trtm(        val(base,  "trtm","TRTM"))
                    .trDcnt(      val(base,  "trDcnt","TR_DCNT"))
                    .perTrco(     val(base,  "perTrco","PER_TRCO"))
                    .instPerTrco( val(base,  "instPerTrco","INST_PER_TRCO"))
                    .inoNm(       val(base,  "inoNm","INO_NM"))
                    .addr1(       val(base,  "addr1","ADDR1"))
                    .addr2(       val(base,  "addr2","ADDR2"))
                    .zipCd(       val(base,  "zipCd","ZIP_CD"))
                    .hpAddr(      val(base,  "hpAddr","HP_ADDR"))
                    .trprChap(    val(base,  "trprChap","TRPR_CHAP"))
                    .trprChapTel( val(base,  "trprChapTel","TRPR_CHAP_TEL"))
                    .trprChapEmail(val(base,  "trprChapEmail","TRPR_CHAP_EMAIL"))
                    .facilities(facilities)
                    .equipments(equipments)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("HRD 상세 응답 파싱 실패", e);
        }
    }



    private static String extractQuery(String url, String key) {
        try {
            var uri = new java.net.URI(url);
            String q = uri.getQuery();
            if (q == null) return null;
            for (String p : q.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2 && kv[0].equals(key)) {
                    return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String firstNonNull(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String val(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        for (String k : keys) {
            JsonNode v = node.path(k);
            if (!v.isMissingNode() && !v.isNull()) return v.asText(null);
        }
        return null;
    }
    private static HrdCourseDetailDto.Facility mapFacility(JsonNode n) {
        return HrdCourseDetailDto.Facility.builder()
                .cstmrId(       val(n,"cstmrId","CSTMR_ID"))
                .trafcltyNm(    val(n,"trafcltyNm","TRAFCLTY_NM"))
                .ocuAcptnNmprCn(val(n,"ocuAcptnNmprCn","OCU_ACPTN_CN","OCU_ACPTN_NMPR_CN"))
                .holdQy(        val(n,"holdQy","HOLD_QY"))
                .fcltyArCn(     val(n,"fcltyArCn","FCLTY_AR_CN"))
                .build();
    }

    private static HrdCourseDetailDto.Equipment mapEquipment(JsonNode n) {
        return HrdCourseDetailDto.Equipment.builder()
                .cstmrNm( val(n,"cstmrNm","CSTMR_NM"))
                .eqpmnNm( val(n,"eqpmnNm","EQPMN_NM"))
                .holdQy(  val(n,"holdQy","HOLD_QY"))
                .build();
    }

    // 여러 후보 키 중 첫 번째로 존재하는 노드 반환
    private static JsonNode node(JsonNode parent, String... keys) {
        if (parent == null) return missing();
        for (String k : keys) {
            JsonNode n = parent.get(k);
            if (n != null && !n.isMissingNode() && !n.isNull()) return n;
        }
        return missing();
    }
    private static JsonNode missing() {
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
    // 배열이면 그대로 iterate, 단건(Object)이면 1개짜리로 래핑, 비어있으면 빈 컬렉션
    private static Iterable<JsonNode> asIterable(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return List.of();
        if (n.isArray()) return n;
        if (n.isObject()) return List.of(n);
        return List.of();
    }

    // 배열이면 0번, 객체면 그대로, 아니면 Missing
    private static JsonNode firstObj(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return missing();
        if (n.isArray()) return n.size() > 0 ? n.get(0) : missing();
        if (n.isObject()) return n;
        return missing();
    }
}
