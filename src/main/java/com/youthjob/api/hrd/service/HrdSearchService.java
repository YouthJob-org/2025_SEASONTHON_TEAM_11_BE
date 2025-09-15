package com.youthjob.api.hrd.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.api.hrd.client.HrdApiClient;
import com.youthjob.api.hrd.domain.HrdCourseCatalog;
import com.youthjob.api.hrd.domain.HrdCourseFull;
import com.youthjob.api.hrd.domain.SavedCourse;
import com.youthjob.api.hrd.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.api.hrd.repository.HrdCourseCatalogRepository;
import com.youthjob.api.hrd.repository.HrdCourseFullRepository;
import com.youthjob.api.hrd.repository.SavedCourseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.max;

@Service
@RequiredArgsConstructor
public class HrdSearchService {
    private final HrdApiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final SavedCourseRepository savedCourseRepository;
    private final UserRepository userRepository;
    private final HrdCourseFullRepository fullRepo;

    private final HrdCourseCatalogRepository catalogRepo;

    // ====== 변경: OPEN API 대신 DB 조회 ======
    public List<HrdCourseDto> search(String startDt, String endDt, int page, int size,
                                     String area1, String ncs1, String sort, String sortCol) {
        var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
        var s = java.time.LocalDate.parse(startDt, fmt);
        var e = java.time.LocalDate.parse(endDt, fmt);

        String sortProp = switch (sortCol) {
            case "2" -> "traStartDate";
            case "3" -> "traEndDate";
            default  -> "traStartDate";
        };
        var direction = "DESC".equalsIgnoreCase(sort)
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;
        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page - 1, 0), size, org.springframework.data.domain.Sort.by(direction, sortProp));


        Specification<HrdCourseCatalog> spec = Specification.allOf(
                betweenDates(s, e),
                (area1 == null || area1.isBlank()) ? null : eqArea(area1),
                (ncs1 == null || ncs1.isBlank()) ? null : startsWithNcs(ncs1)
        );

        var pageRes = catalogRepo.findAll(spec, pageable);

        return pageRes.stream().map(c ->
                HrdCourseDto.builder()
                        .title(c.getTitle())
                        .subTitle(c.getSubTitle())
                        .address(c.getAddress())
                        .telNo(c.getTelNo())
                        .traStartDate(c.getTraStartDate().format(fmt))
                        .traEndDate(c.getTraEndDate().format(fmt))
                        .trainTarget(c.getTrainTarget())
                        .trainTargetCd(c.getTrainTargetCd())
                        .ncsCd(c.getNcsCd())
                        .trprId(c.getTrprId())
                        .trprDegr(c.getTrprDegr())
                        .courseMan(c.getCourseMan())
                        .realMan(c.getRealMan())
                        .yardMan(c.getYardMan())
                        .titleLink(c.getTitleLink())
                        .subTitleLink(c.getSubTitleLink())
                        .torgId(c.getTorgId())
                        .build()
        ).toList();
    }

    private org.springframework.data.jpa.domain.Specification<com.youthjob.api.hrd.domain.HrdCourseCatalog>
    betweenDates(java.time.LocalDate s, java.time.LocalDate e) {
        return (root, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("traEndDate"), s),   // 종료일이 시작일 이후
                cb.lessThanOrEqualTo(root.get("traStartDate"), e)     // 시작일이 종료일 이전 (기간 겹침)
        );
    }
    private org.springframework.data.jpa.domain.Specification<com.youthjob.api.hrd.domain.HrdCourseCatalog>
    eqArea(String area1) {
        return (root, q, cb) -> cb.equal(root.get("area1"), area1);
    }
    private org.springframework.data.jpa.domain.Specification<com.youthjob.api.hrd.domain.HrdCourseCatalog>
    startsWithNcs(String ncs1) {
        return (root, q, cb) -> cb.like(root.get("ncsCd"), ncs1 + "%");
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
                    .filePath(    val(base,  "filePath","FILE_PATH"))
                    .pFileName(   val(base,  "pFileName","P_FILE_NAME"))
                    .torgParGrad(val(base,"torgParGrad","TORG_PAR_GRAD"))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("HRD 상세 응답 파싱 실패", e);
        }
    }

    public List<HrdCourseStatDto> getStats(String trprId, String torgId, String trprDegrOrNull) {
        String body = client.getStatsXml(trprId, torgId, trprDegrOrNull);
        if (body == null || body.isBlank()) return List.of();

        try {
            XmlMapper xml = new XmlMapper();
            JsonNode doc = xml.readTree(body.getBytes(StandardCharsets.UTF_8));


            JsonNode hr = doc.has("HRDNet") ? doc.get("HRDNet") : doc;


            JsonNode list =
                    hr.has("scn_list") ? hr.get("scn_list")
                            : hr.has("SCN_LIST") ? hr.get("SCN_LIST")
                            : doc.has("scn_list") ? doc.get("scn_list")
                            : missing(); // <- 네가 갖고 있는 util

            List<HrdCourseStatDto> out = new ArrayList<>();
            for (JsonNode n : asIterable(list)) { // <- 단건/배열 모두 처리되는 util
                out.add(HrdCourseStatDto.builder()
                        .trprId(       val(n,"trprId","TRPR_ID"))
                        .trprDegr(     val(n,"trprDegr","TRPR_DEGR"))
                        .trprNm(       val(n,"trprNm","TRPR_NM"))
                        .instIno(      val(n,"instIno","INST_INO"))
                        .trStaDt(      val(n,"trStaDt","TR_STA_DT"))
                        .trEndDt(      val(n,"trEndDt","TR_END_DT"))
                        .totFxnum(     val(n,"totFxnum","TOT_FXNUM"))
                        .totParMks(    val(n,"totParMks","TOT_PAR_MKS"))
                        .totTrpCnt(    val(n,"totTrpCnt","TOT_TRP_CNT"))
                        .finiCnt(      val(n,"finiCnt","FINI_CNT"))
                        .totTrco(      val(n,"totTrco","TOT_TRCO"))
                        .eiEmplRate3(  val(n,"eiEmplRate3","EI_EMPL_RATE_3"))
                        .eiEmplCnt3(   val(n,"eiEmplCnt3","EI_EMPL_CNT_3"))
                        .eiEmplRate6(  val(n,"eiEmplRate6","EI_EMPL_RATE_6"))
                        .eiEmplCnt6(   val(n,"eiEmplCnt6","EI_EMPL_CNT_6"))
                        .hrdEmplRate6( val(n,"hrdEmplRate6","HRD_EMPL_RATE_6"))
                        .hrdEmplCnt6(  val(n,"hrdEmplCnt6","HRD_EMPL_CNT_6"))
                        .build());
            }
            return out;

        } catch (Exception e) {
            // XML 파싱 실패 시에도 500 대신 빈 배열 (로그만 남겨도 OK)
            return List.of();
        }
    }

    @jakarta.transaction.Transactional
    public HrdCourseFullDto getCourseFull(String trprId, String trprDegr, String torgId) {
        HrdCourseDetailDto detail = getDetail(trprId, trprDegr, torgId);
        java.util.List<HrdCourseStatDto> stats = getStats(trprId, torgId, trprDegr);


        upsertFull(trprId, trprDegr, torgId, detail, stats);

        return HrdCourseFullDto.builder()
                .detail(detail)
                .stats(stats)
                .build();
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

        // 중복 방지(유니크 제약 + 사전 체크)
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

    /** 목록 */
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

    /** 토글: 없으면 저장, 있으면 삭제 후 null 반환 */
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
            return null;
        }
        return saveCourse(req);
    }

    // 업서트 메서드
    @jakarta.transaction.Transactional
    public void upsertFull(String trprId, String trprDegr, String torgId,
                           HrdCourseDetailDto detail, java.util.List<HrdCourseStatDto> stats) {

        var e = fullRepo.findByTrprIdAndTrprDegrAndTorgId(trprId, trprDegr, torgId)
                .orElseGet(() -> HrdCourseFull.builder()
                        .trprId(trprId).trprDegr(trprDegr).torgId(torgId)
                        .build());

        e.applyDetail(detail);
        e.setStats(stats);
        e.setSavedAt(java.time.Instant.now());

        fullRepo.save(e);
    }


}
