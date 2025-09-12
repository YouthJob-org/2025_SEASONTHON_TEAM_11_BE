package com.youthjob.api.empprogram.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.youthjob.api.empprogram.client.EmpProgramApiClient;
import com.youthjob.api.empprogram.domain.EmpProgramCatalog;
import com.youthjob.api.empprogram.dto.EmpProgramItemDto;
import com.youthjob.api.empprogram.dto.EmpProgramResponseDto;
import com.youthjob.api.empprogram.repository.EmpProgramCatalogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.Math.ceil;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class EmpProgramCatalogService {

    private final EmpProgramApiClient client;
    private final EmpProgramCatalogRepository repo;

    private static final XmlMapper XML = new XmlMapper();
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMdd");


    @Value("#{'${emp.centers.topOrgCds:12010,13000,14010,15000,16000,17000}'.split(',')}")
    private List<String> topOrgCds;


    public static String todayYyyymmdd() {
        return LocalDate.now(KST).format(F);
    }


    private static LocalDate parseDay(String yyyymmdd) { return LocalDate.parse(yyyymmdd, F); }


    static String buildExtKey(EmpProgramItemDto r) {
        return String.join("|",
                nv(r.getOrgNm()), nv(r.getPgmNm()), nv(r.getPgmSubNm()),
                nv(r.getPgmStdt()), nv(r.getPgmEndt()), nv(r.getOpenTimeClcd()), nv(r.getOpenTime())
        );
    }
    private static String nv(String s) { return s == null ? "" : s; }


    @Transactional
    public int harvestAllForDay(String yyyymmdd) {
        int totalInserted = 0;

        // 먼저 1페이지 조회해 total 파악
        EmpProgramResponseDto first = fetch(yyyymmdd, 1, 100);
        int total = first.getTotal() == null ? 0 : first.getTotal();
        int display = (first.getDisplay() == null || first.getDisplay() < 1) ? 100 : first.getDisplay();
        int lastPage = Math.min(1000, (int) ceil(total / (double) display));

        totalInserted += upsert(first.getPrograms());

        // 2페이지~마지막 페이지
        for (int page = 2; page <= lastPage; page++) {
            EmpProgramResponseDto pageDto = fetch(yyyymmdd, page, display);
            totalInserted += upsert(pageDto.getPrograms());
        }

        return totalInserted;
    }


    @Transactional
    public int harvestAllForDayByOrg(String yyyymmdd, String topOrgCd, String orgCd) {
        int totalInserted = 0;

        EmpProgramResponseDto first = fetch(yyyymmdd, topOrgCd, orgCd, 1, 100);
        int total = first.getTotal() == null ? 0 : first.getTotal();
        int display = (first.getDisplay() == null || first.getDisplay() < 1) ? 100 : first.getDisplay();
        int lastPage = Math.min(1000, (int) Math.ceil(total / (double) display));

        totalInserted += upsert(first.getPrograms());
        for (int page = 2; page <= lastPage; page++) {
            totalInserted += upsert(fetch(yyyymmdd, topOrgCd, orgCd, page, display).getPrograms());
        }
        return totalInserted;
    }


    @Transactional
    public int harvestAllForDayPartitioned(String yyyymmdd) {
        int inserted = 0;
        for (String topOrgCd : topOrgCds) {
            inserted += harvestAllForDayByOrg(yyyymmdd, topOrgCd, null);
        }
        return inserted;
    }


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

    //이미 시작된 교육은 삭제처리(매일 자정)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void nightlyRefresh() {
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

    /** 없으면 insert(간단 upsert). 대량일 때는 배치 insert 고려 */
    private int upsert(List<EmpProgramItemDto> items) {
        if (items == null || items.isEmpty()) return 0;

        List<EmpProgramCatalog> toInsert = items.stream()
                .filter(it -> it.getPgmStdt() != null && !it.getPgmStdt().isBlank())
                .filter(it -> !repo.existsByExtKey(buildExtKey(it)))
                .map(it -> EmpProgramCatalog.builder()
                        .extKey(buildExtKey(it))
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

        if (!toInsert.isEmpty()) {
            repo.saveAll(toInsert);
        }
        return toInsert.size();
    }
}
