package com.youthjob.api.hrd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.api.hrd.client.HrdApiClient;
import com.youthjob.api.hrd.domain.HrdCourseCatalog;
import com.youthjob.api.hrd.repository.HrdCourseCatalogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrdCatalogSyncService {

    @Autowired
    private ThreadPoolTaskExecutor hrdExecutor; // @Bean(name="hrdExecutor") 로 등록된 풀

    private final HrdApiClient client;
    private final HrdCourseCatalogRepository repo;
    private final HrdSearchService hrdSearchService; // getCourseFull() 내부에서 DB 업서트 수행

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HRD_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /* ===================== 카탈로그(요약) 동기화 ===================== */

    @Transactional
    public int harvestMonthsAhead(int months, String area1, String ncs1) {
        LocalDate today = LocalDate.now(KST);
        LocalDate end   = today.plusMonths(months);

        int totalUpserts = 0;
        LocalDate cursor = today;
        while (!cursor.isAfter(end)) {
            LocalDate winStart = cursor;
            LocalDate winEnd   = cursor.plusMonths(1).minusDays(1);
            if (winEnd.isAfter(end)) winEnd = end;

            totalUpserts += harvestWindow(winStart, winEnd, area1, ncs1);
            cursor = cursor.plusMonths(1);
        }
        return totalUpserts;
    }

    @Transactional
    public int harvestWindow(LocalDate start, LocalDate end, String area1, String ncs1) {
        int size = 100;
        int page = 1;
        int windowUpserts = 0;

        while (true) {
            int cnt = harvestPage(start, end, page, size, area1, ncs1, "ASC", "2");
            windowUpserts += cnt;

            if (cnt < size) break; // 마지막 페이지
            page++;
            if (page > 1000) { // 안전장치
                log.warn("Too many pages, stop at page={}", page);
                break;
            }
        }
        log.info("harvestWindow {}~{} upserts={}", start, end, windowUpserts);
        return windowUpserts;
    }

    @Transactional
    public int harvestPage(LocalDate start, LocalDate end, int page, int size,
                           String area1, String ncs1, String sort, String sortCol) {

        String json = client.search(start.format(HRD_FMT), end.format(HRD_FMT),
                page, size, area1, ncs1, sort, sortCol);

        int upserts = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            // 유연한 목록 추출
            JsonNode list = root.path("HRDNet").path("srchList").path("scn_list");
            if (!list.isArray()) {
                if (root.has("srchList")) {
                    JsonNode s = root.get("srchList");
                    if (s.isArray()) list = s;
                    else if (s.has("scn_list")) list = s.get("scn_list");
                    else if (s.has("list")) list = s.get("list");
                }
            }
            if (!list.isArray()) return 0;

            Instant now = Instant.now();
            for (JsonNode n : list) {
                String trprId   = txt(n, "trprId");
                String trprDegr = txt(n, "trprDegr");
                if (isBlank(trprId) || isBlank(trprDegr)) continue;

                LocalDate sdt = parseDate(txt(n, "traStartDate"));
                LocalDate edt = parseDate(txt(n, "traEndDate"));
                if (sdt == null || edt == null) continue;

                // torgId 후보
                String torgId = firstNonBlank(
                        txt(n, "torgId"), txt(n, "instIno"), txt(n, "cstmrId"), txt(n, "trainstCstmrId")
                );

                HrdCourseCatalog e = repo.findByTrprIdAndTrprDegr(trprId, trprDegr)
                        .orElseGet(HrdCourseCatalog::new);

                e.setTrprId(trprId);
                e.setTrprDegr(trprDegr);
                e.setTorgId(torgId);
                e.setTraStartDate(sdt);
                e.setTraEndDate(edt);

                e.setArea1(firstNonBlank(txt(n, "srchTraArea1"), txt(n, "area1")));
                e.setNcsCd(txt(n, "ncsCd"));

                e.setTitle(txt(n, "title"));
                e.setSubTitle(txt(n, "subTitle"));
                e.setAddress(txt(n, "address"));
                e.setTelNo(txt(n, "telNo"));
                e.setTrainTarget(txt(n, "trainTarget"));
                e.setTrainTargetCd(txt(n, "trainTargetCd"));
                e.setCourseMan(txt(n, "courseMan"));
                e.setRealMan(txt(n, "realMan"));
                e.setYardMan(txt(n, "yardMan"));
                e.setTitleLink(txt(n, "titleLink"));
                e.setSubTitleLink(txt(n, "subTitleLink"));

                e.setSyncedAt(now);
                repo.save(e);
                upserts++;
            }
        } catch (Exception ex) {
            log.warn("harvestPage parse error: {}", ex.getMessage());
        }
        return upserts;
    }

    @Transactional
    public int purgeEnded() {
        LocalDate today = LocalDate.now(KST);
        return repo.deleteAllEndedBefore(today);
    }

    /** 매주 토요일 00:00 KST: 종료 삭제 → 6개월 카탈로그 갱신 → area1 백필 → 상세/통계 저장 */
    @Scheduled(cron = "0 0 0 * * SUN", zone = "Asia/Seoul")
    @Transactional
    public void weeklyRefresh() {
        int deleted = purgeEnded();                         // 지난 과정 정리
        int upserts = harvestMonthsAhead(6, null, null);    // 6개월 요약 수집
        hrdSearchService.backfillArea1InDb();               // area1 주소 기반 보정

        // Full(상세+교육기관 정보) 저장
        int processedFull = harvestFullMonthsAheadParallel(6, null, null, 500, 0, 0);

        log.info("[HRD weekly] deleted={}, catalogUpserts={}, fullProcessed={}",
                deleted, upserts, processedFull);
    }

    //FULL(상세+통계) 저장: 병렬
    public int harvestFullMonthsAheadParallel(int months, String area1, String ncs1,
                                              int pageSize, int maxItems, int concurrencyIgnored) {
        LocalDate today = LocalDate.now(KST);
        LocalDate end   = today.plusMonths(months);

        Specification<HrdCourseCatalog> spec = Specification.allOf(
                betweenDates(today, end),
                (area1 == null || area1.isBlank()) ? null : eqArea(area1),
                (ncs1  == null || ncs1.isBlank())  ? null : startsWithNcs(ncs1)
        );

        int processed = 0;
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, Math.max(pageSize,1), Sort.by("traStartDate").ascending());
            Page<HrdCourseCatalog> slice = repo.findAll(spec, pageable);
            if (slice.isEmpty()) break;

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (HrdCourseCatalog c : slice) {
                String torgId = ensureTorgId(c);
                if (isBlank(torgId)) continue;

                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        hrdSearchService.getCourseFull(c.getTrprId(), c.getTrprDegr(), torgId);
                    } catch (Exception ex) {
                        log.warn("harvest-full fail trprId={}, degr={}, torgId={}, msg={}",
                                c.getTrprId(), c.getTrprDegr(), torgId, ex.getMessage());
                    }
                }, hrdExecutor));

                processed++;
                if (maxItems > 0 && processed >= maxItems) break;
            }

            futures.forEach(CompletableFuture::join);

            if (maxItems > 0 && processed >= maxItems) break;
            if (!slice.hasNext()) break;
            page++;
        }

        log.info("harvest-full-parallel done: processed={}", processed);
        return processed;
    }

    /* ===================== Spec & Util ===================== */

    private Specification<HrdCourseCatalog> betweenDates(LocalDate s, LocalDate e) {
        return (root, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("traEndDate"), s),
                cb.lessThanOrEqualTo(root.get("traStartDate"), e)
        );
    }

    private Specification<HrdCourseCatalog> eqArea(String area1) {
        return (root, q, cb) -> cb.equal(root.get("area1"), area1);
    }

    private Specification<HrdCourseCatalog> startsWithNcs(String ncs1) {
        return (root, q, cb) -> cb.like(root.get("ncsCd"), ncs1 + "%");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String txt(JsonNode n, String k) {
        JsonNode v = n.get(k);
        return (v == null || v.isNull()) ? null : v.asText(null);
    }

    /** yyyyMMdd 또는 yyyy-MM-dd 모두 지원 */
    private static LocalDate parseDate(String v) {
        if (isBlank(v)) return null;
        String digits = v.replaceAll("\\D", "");
        if (digits.length() == 8) return LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);
        return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String firstNonBlank(String... xs) {
        if (xs == null) return null;
        for (String s : xs) if (!isBlank(s)) return s;
        return null;
    }

    /** torgId 없을 때 링크에서 역추출하고 가능하면 백필(save) */
    private String ensureTorgId(HrdCourseCatalog c) {
        String torgId = c.getTorgId();
        if (!isBlank(torgId)) return torgId;

        torgId = extractTorgIdFromLinks(c);
        if (!isBlank(torgId)) {
            try {
                c.setTorgId(torgId);
                repo.save(c);
                log.debug("backfilled torgId: trprId={}, degr={}, torgId={}",
                        c.getTrprId(), c.getTrprDegr(), torgId);
            } catch (Exception ex) {
                log.warn("failed to backfill torgId: trprId={}, degr={}, err={}",
                        c.getTrprId(), c.getTrprDegr(), ex.getMessage());
            }
        }
        return torgId;
    }

    private static String extractTorgIdFromLinks(HrdCourseCatalog c) {
        return firstNonBlank(
                extractQuery(c.getTitleLink(), "trainstCstmrId"),
                extractQuery(c.getTitleLink(), "srchTorgId"),
                extractQuery(c.getTitleLink(), "cstmrId"),
                extractQuery(c.getSubTitleLink(), "trainstCstmrId"),
                extractQuery(c.getSubTitleLink(), "srchTorgId"),
                extractQuery(c.getSubTitleLink(), "cstmrId")
        );
    }

    private static String extractQuery(String url, String key) {
        try {
            if (isBlank(url)) return null;
            URI uri = new URI(url);
            String q = uri.getQuery();
            if (q == null) return null;
            for (String p : q.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2 && kv[0].equals(key)) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}
