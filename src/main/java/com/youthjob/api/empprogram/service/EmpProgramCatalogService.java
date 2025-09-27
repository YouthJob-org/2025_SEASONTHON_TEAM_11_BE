package com.youthjob.api.empprogram.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.youthjob.api.empprogram.client.EmpProgramApiClient;
import com.youthjob.api.empprogram.config.EmpCentersProps;
import com.youthjob.api.empprogram.domain.EmpProgramCatalog;
import com.youthjob.api.empprogram.dto.EmpProgramItemDto;
import com.youthjob.api.empprogram.dto.EmpProgramResponseDto;
import com.youthjob.api.empprogram.repository.EmpProgramCatalogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static java.lang.Math.ceil;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpProgramCatalogService {

    private final EmpProgramApiClient client;
    private final EmpProgramCatalogRepository repo;
    private final EmpCentersProps centersProps;

    private static final XmlMapper XML = new XmlMapper();
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 운영에서 바꾸기 쉽도록 프로퍼티로 분리 (없으면 기본 6개 지방청 코드 사용) */
    @Value("#{'${emp.centers.topOrgCds:12010,13000,14010,15000,16000,17000}'.split(',')}")
    private List<String> topOrgCds;

    /** 오늘(한국시간) yyyymmdd */
    public static String todayYyyymmdd() {
        return LocalDate.now(KST).format(F);
    }

    /** yyyymmdd -> LocalDate */
    private static LocalDate parseDay(String yyyymmdd) { return LocalDate.parse(yyyymmdd, F); }

    /** extKey 규칙(저장목록과 동일한 구성으로 충돌 최소화) */
    static String buildExtKey(EmpProgramItemDto r) {
        return String.join("|",
                nv(r.getOrgNm()), nv(r.getPgmNm()), nv(r.getPgmSubNm()),
                nv(r.getPgmStdt()), nv(r.getPgmEndt()), nv(r.getOpenTimeClcd()), nv(r.getOpenTime())
        );
    }
    private static String nv(String s) { return s == null ? "" : s; }

    /** 단일 일자 전체 수집(페이지 전부) — (fallback, 코드컨텍스트 없이 저장) */
    @Transactional
    public int harvestAllForDay(String yyyymmdd) {
        int totalInserted = 0;

        EmpProgramResponseDto first = fetch(yyyymmdd, 1, 100);
        int total = first.getTotal() == null ? 0 : first.getTotal();
        int display = (first.getDisplay() == null || first.getDisplay() < 1) ? 100 : first.getDisplay();
        int lastPage = Math.min(1000, (int) ceil(total / (double) display));

        totalInserted += upsert(first.getPrograms()); // ← 코드컨텍스트 없음(fallback)

        for (int page = 2; page <= lastPage; page++) {
            EmpProgramResponseDto pageDto = fetch(yyyymmdd, page, display);
            totalInserted += upsert(pageDto.getPrograms()); // ← 코드컨텍스트 없음(fallback)
        }
        return totalInserted;
    }

    /** 날짜 + (지방청/센터) 분할 수집 — 권장 경로 */
    @Transactional
    public int harvestAllForDayByOrg(String yyyymmdd, String topOrgCd, String orgCd) {
        int totalInserted = 0;

        EmpProgramResponseDto first = fetch(yyyymmdd, topOrgCd, orgCd, 1, 100);
        int total = first.getTotal() == null ? 0 : first.getTotal();
        int display = (first.getDisplay() == null || first.getDisplay() < 1) ? 100 : first.getDisplay();
        int lastPage = Math.min(1000, (int) Math.ceil(total / (double) display));

        totalInserted += upsert(first.getPrograms(), topOrgCd, orgCd);
        for (int page = 2; page <= lastPage; page++) {
            EmpProgramResponseDto dto = fetch(yyyymmdd, topOrgCd, orgCd, page, display);
            totalInserted += upsert(dto.getPrograms(), topOrgCd, orgCd);
        }
        return totalInserted;
    }

    // EmpProgramCatalogService.java
    @Value("#{${emp.centers.byTop:{}}}")
    private java.util.Map<String, String> centersByTop; // "CSV 문자열" 맵

    @Transactional
    public int harvestAllForDayPartitioned(String yyyymmdd) {
        List<String> topOrgCds = centersProps.topOrgCdList();
        Map<String, String> byTop = centersProps.getByTop();   // 절대 null 아님(기본 emptyMap)

        if (topOrgCds.isEmpty()) {
            log.warn("topOrgCds is empty — fallback to default 6 tops");
            topOrgCds = List.of("12010","13000","14010","15000","16000","17000");
        }

        int inserted = 0;
        for (String top : topOrgCds) {
            String csv = byTop.get(top);
            if (csv == null || csv.isBlank()) {
                log.warn("No center list for topOrgCd {}. Fallback to top-only harvesting.", top);
                inserted += harvestAllForDayByOrg(yyyymmdd, top, null);
            } else {
                for (String org : csv.split(",")) {
                    String orgTrim = org.trim();
                    if (!orgTrim.isEmpty()) {
                        inserted += harvestAllForDayByOrg(yyyymmdd, top, orgTrim);
                    }
                }
            }
        }
        return inserted;
    }


    /** 오늘~6개월(-1일) 윈도우 전체 수집 (분할 수집 사용) */
    @Transactional
    public int harvestRollingSixMonths(String baseDayYyyymmdd) {
        LocalDate start = parseDay(baseDayYyyymmdd);
        LocalDate endExclusive = start.plusMonths(6);
        int inserted = 0;

        for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
            inserted += harvestAllForDayPartitioned(d.format(F));
        }
        return inserted;
    }


    @Transactional
    public int purgePast(String todayYyyymmdd) {
        return repo.deleteAllEndedBefore(todayYyyymmdd);
    }

    // 매주 일요일 0시(자정)에 실행
    @Scheduled(cron = "0 0 0 * * SUN", zone = "Asia/Seoul")
    @Transactional
    public void weeklyRefresh() {
        String today = todayYyyymmdd();
        purgePast(today);
        harvestRollingSixMonths(today);
    }


    /** 기존 호출부 호환용(3파라미터) */
    private EmpProgramResponseDto fetch(String yyyymmdd, int startPage, int display) {
        return fetch(yyyymmdd, null, null, startPage, display);
    }

    /** 실제 호출(5파라미터) */
    private EmpProgramResponseDto fetch(String yyyymmdd, String topOrgCd, String orgCd,
                                        int startPage, int display) {
        String xml = client.getProgramsXml(yyyymmdd, topOrgCd, orgCd, startPage, display);
        try {
            EmpProgramResponseDto dto = XML.readValue(xml, EmpProgramResponseDto.class);
            if (dto.getPrograms() == null) {
                dto = EmpProgramResponseDto.builder()
                        .total(dto.getTotal())
                        .startPage(dto.getStartPage())
                        .display(dto.getDisplay())
                        .message(dto.getMessage())
                        .messageCd(dto.getMessageCd())
                        .programs(List.of())
                        .build();
            }
            return dto;
        } catch (Exception e) {
            return EmpProgramResponseDto.builder()
                    .total(0).startPage(startPage).display(display)
                    .message("XML parse error: " + e.getMessage())
                    .messageCd("PARSE_ERROR")
                    .programs(List.of())
                    .build();
        }
    }

    /** 없으면 insert(간단 upsert). 대량일 때는 배치 insert 고려 — (컨텍스트 없는 fallback) */
    private int upsert(List<EmpProgramItemDto> items) {
        return upsert(items, null, null);
    }

    /** 없으면 insert(간단 upsert). (topOrgCd/orgCd 컨텍스트 포함 버전) */
    private int upsert(List<EmpProgramItemDto> items, String topOrgCd, String orgCd) {
        if (items == null || items.isEmpty()) return 0;

        List<EmpProgramCatalog> toInsert = items.stream()
                .filter(it -> it.getPgmStdt() != null && !it.getPgmStdt().isBlank())
                .filter(it -> !repo.existsByExtKey(buildExtKey(it)))
                .map(it -> EmpProgramCatalog.builder()
                        .extKey(buildExtKey(it))
                        .topOrgCd(topOrgCd)
                        .orgCd(orgCd)
                        .orgNm(it.getOrgNm())
                        .pgmNm(it.getPgmNm())
                        .pgmSubNm(it.getPgmSubNm())
                        .pgmTarget(it.getPgmTarget())
                        .pgmStdt(it.getPgmStdt())
                        .pgmEndt(it.getPgmEndt())
                        .openTimeClcd(it.getOpenTimeClcd())
                        .openTime(it.getOpenTime())
                        .operationTime(it.getOperationTime())
                        .openPlcCont(it.getOpenPlcCont())
                        .build())
                .collect(toList());

        if (!toInsert.isEmpty()) repo.saveAll(toInsert);
        return toInsert.size();
    }
}
