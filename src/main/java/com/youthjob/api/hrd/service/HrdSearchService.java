package com.youthjob.api.hrd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.api.hrd.client.HrdApiClient;
import com.youthjob.api.hrd.domain.HrdCourseCatalog;
import com.youthjob.api.hrd.domain.HrdCourseFull;
import com.youthjob.api.hrd.domain.SavedCourse;
import com.youthjob.api.hrd.dto.*;
import com.youthjob.api.hrd.repository.HrdCourseCatalogRepository;
import com.youthjob.api.hrd.repository.HrdCourseFullRepository;
import com.youthjob.api.hrd.repository.SavedCourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class HrdSearchService {

    private final HrdApiClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final HrdCourseCatalogRepository catalogRepo;
    private final HrdCourseFullRepository fullRepo;

    private final SavedCourseRepository savedCourseRepository;
    private final UserRepository userRepository;

    public record SliceResponse<T>(List<T> content, boolean hasNext) {}

    /* ======================= 검색(카탈로그) - DB ======================= */
    @Transactional(readOnly = true)
    public SliceResponse<HrdCourseDto> search(String startDt, String endDt, int page, int size,
                                     String area1, String ncs1, String sort, String sortCol) {
        var fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        var s = LocalDate.parse(startDt, fmt);
        var e = LocalDate.parse(endDt, fmt);

        var sortProp = "3".equals(sortCol) ? "traEndDate" : "traStartDate";
        var direction = "DESC".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(direction, sortProp));

        Slice<HrdCourseRow> slice = (area1 != null && !area1.isBlank())
                ? catalogRepo.findSliceByArea1(s, e, area1, blankToNull(ncs1), pageable)
                : catalogRepo.findSliceNoArea1(s, e, blankToNull(ncs1), pageable);


        var list = slice.stream().map(r -> HrdCourseDto.builder()
                .title(r.getTitle())
                .subTitle(r.getSubTitle())
                .address(r.getAddress())
                .telNo(r.getTelNo())
                .traStartDate(r.getTraStartDate().format(fmt))
                .traEndDate(r.getTraEndDate().format(fmt))
                .trainTarget(r.getTrainTarget())
                .trainTargetCd(r.getTrainTargetCd())
                .ncsCd(r.getNcsCd())
                .trprId(r.getTrprId())
                .trprDegr(r.getTrprDegr())
                .courseMan(r.getCourseMan())
                .realMan(r.getRealMan())
                .yardMan(r.getYardMan())
                .titleLink(r.getTitleLink())
                .subTitleLink(r.getSubTitleLink())
                .torgId(r.getTorgId())
                .build()
        ).toList();
        return new SliceResponse<>(list, slice.hasNext());
    }

    private String blankToNull(String s){ return (s==null||s.isBlank())?null:s; }


    /* ======================= 상세/통계 - DB 우선 ======================= */





    /** 상세+통계 묶음: DB 우선 → 없으면 API 호출 후 업서트하고 반환 */
    @Transactional
    public HrdCourseFullDto getCourseFull(String trprId, String trprDegr, String torgId) {
        var found = fullRepo.findByTrprIdAndTrprDegrAndTorgId(trprId, trprDegr, torgId);
        if (found.isPresent()) return toFullDto(found.get());

        var detail = fetchDetailFromApi(trprId, trprDegr, torgId);
        var stats  = fetchStatsFromApi(trprId, torgId, trprDegr);

        upsertFull(trprId, trprDegr, torgId, detail, stats);

        var saved = fullRepo.findByTrprIdAndTrprDegrAndTorgId(trprId, trprDegr, torgId).orElseThrow();
        return toFullDto(saved);
    }

    /* ======================= Saved (즐겨찾기) ======================= */

    @Transactional
    public SavedCourseDto saveCourse(SaveCourseRequest req) {
        User me = getCurrentUser();

        if (savedCourseRepository.existsByUserAndTrprIdAndTrprDegr(me, req.trprId(), req.trprDegr())) {
            return savedCourseRepository.findTopByUserAndTrprIdAndTrprDegrOrderByCreatedAtDesc(me, req.trprId(), req.trprDegr())
                    .map(SavedCourseDto::from)
                    .orElseThrow();
        }

        var saved = SavedCourse.builder()
                .user(me)
                .trprId(req.trprId())
                .trprDegr(req.trprDegr())
                .torgId(req.torgId())
                .build();

        return SavedCourseDto.from(savedCourseRepository.save(saved));
    }

    public List<SavedCourseView> listSaved() {
        User me = getCurrentUser();
        var rows = savedCourseRepository.findAllViewsByUser(me);
        return rows;
    }


    @Transactional
    public void deleteSaved(Long id) {
        User me = getCurrentUser();
        var target = savedCourseRepository.findByIdAndUser(id, me)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 항목을 찾을 수 없습니다."));
        savedCourseRepository.delete(target);
    }

    @Transactional
    public SavedCourseDto toggleSaved(SaveCourseRequest req) {
        User me = getCurrentUser();
        var exists = savedCourseRepository.existsByUserAndTrprIdAndTrprDegr(me, req.trprId(), req.trprDegr());
        if (exists) { //이미 저장된 교육이면 삭제
            var target = savedCourseRepository.findAllByUserOrderByCreatedAtDesc(me).stream()
                    .filter(c -> c.getTrprId().equals(req.trprId()) && c.getTrprDegr().equals(req.trprDegr()))
                    .findFirst().orElseThrow();
            savedCourseRepository.delete(target);
            return null;
        }
        return saveCourse(req);
    }

    /* ======================= 내부 유틸/업서트 ======================= */

    @Transactional
    public void upsertFull(String trprId, String trprDegr, String torgId,
                           HrdCourseDetailDto detail, List<HrdCourseStatDto> stats) {

        var e = fullRepo.findByTrprIdAndTrprDegrAndTorgId(trprId, trprDegr, torgId)
                .orElseGet(() -> HrdCourseFull.builder()
                        .trprId(trprId).trprDegr(trprDegr).torgId(torgId)
                        .build());

        e.applyDetail(detail);
        e.setStats(stats == null ? List.of() : stats);
        e.setSavedAt(java.time.Instant.now());
        fullRepo.save(e);
    }

    private HrdCourseDetailDto fetchDetailFromApi(String trprId, String trprDegr, String torgId) {
        String json = client.getDetail(trprId, trprDegr, torgId);
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode hr = root.has("HRDNet") ? root.get("HRDNet") : root;

            JsonNode base   = firstObj(node(hr, "inst_base_info",   "INST_BASE_INFO"));
            JsonNode detail = firstObj(node(hr, "inst_detail_info", "INST_DETAIL_INFO"));

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
                    .torgParGrad( val(base,  "torgParGrad","TORG_PAR_GRAD"))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("HRD 상세 응답 파싱 실패", e);
        }
    }

    private List<HrdCourseStatDto> fetchStatsFromApi(String trprId, String torgId, String trprDegrOrNull) {
        String body = client.getStatsXml(trprId, torgId, trprDegrOrNull);
        if (body == null || body.isBlank()) return List.of();

        try {
            XmlMapper xml = new XmlMapper();
            JsonNode doc = xml.readTree(body.getBytes(StandardCharsets.UTF_8));

            JsonNode hr = doc.has("HRDNet") ? doc.get("HRDNet") : doc;
            JsonNode list = hr.has("scn_list") ? hr.get("scn_list")
                    : hr.has("SCN_LIST") ? hr.get("SCN_LIST")
                    : doc.has("scn_list") ? doc.get("scn_list")
                    : missing();

            List<HrdCourseStatDto> out = new ArrayList<>();
            for (JsonNode n : asIterable(list)) {
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
            return List.of();
        }
    }

    private HrdCourseDetailDto toDetailDto(HrdCourseFull e) {
        return HrdCourseDetailDto.builder()
                .trprId(e.getTrprId())
                .trprDegr(e.getTrprDegr())
                .trprNm(e.getTrprNm())
                .ncsCd(e.getNcsCd())
                .ncsNm(e.getNcsNm())
                .traingMthCd(e.getTraingMthCd())
                .trtm(e.getTrtm())
                .trDcnt(e.getTrDcnt())
                .perTrco(e.getPerTrco())
                .instPerTrco(e.getInstPerTrco())
                .inoNm(e.getInoNm())
                .addr1(e.getAddr1())
                .addr2(e.getAddr2())
                .zipCd(e.getZipCd())
                .hpAddr(e.getHpAddr())
                .trprChap(e.getTrprChap())
                .trprChapTel(e.getTrprChapTel())
                .trprChapEmail(e.getTrprChapEmail())
                .filePath(e.getFilePath())
                .pFileName(e.getPFileName())
                .torgParGrad(e.getTorgParGrad())
                .build();
    }

    private HrdCourseFullDto toFullDto(HrdCourseFull e) {
        return HrdCourseFullDto.builder()
                .detail(toDetailDto(e))
                .stats(e.getStats() == null ? List.of() : e.getStats())
                .build();
    }

    /* ======================= JSON/XML 파싱 유틸 ======================= */

    private static String firstNonNull(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.isBlank()) return v;
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

    private static JsonNode node(JsonNode parent, String... keys) {
        if (parent == null) return missing();
        for (String k : keys) {
            JsonNode n = parent.get(k);
            if (n != null && !n.isMissingNode() && !n.isNull()) return n;
        }
        return missing();
    }

    private static JsonNode firstObj(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return missing();
        if (n.isArray()) return n.size() > 0 ? n.get(0) : missing();
        if (n.isObject()) return n;
        return missing();
    }

    private static Iterable<JsonNode> asIterable(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return List.of();
        if (n.isArray()) return n;
        if (n.isObject()) return List.of(n);
        return List.of();
    }

    private static JsonNode missing() {
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    /* ======================= 인증 유틸 ======================= */

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + auth.getName()));
    }

    @Transactional
    public int backfillArea1InDb() {
        int updated = catalogRepo.bulkBackfillArea1();
        log.info("area1 backfill done: updated={}", updated);
        return updated;
    }
}
