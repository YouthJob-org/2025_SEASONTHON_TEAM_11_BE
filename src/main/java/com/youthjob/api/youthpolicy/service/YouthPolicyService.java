package com.youthjob.api.youthpolicy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import com.youthjob.api.youthpolicy.domain.YouthPolicy;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto.Policy;
import com.youthjob.api.youthpolicy.dto.YouthPolicyDetailDto;
import com.youthjob.api.youthpolicy.repository.YouthPolicyRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouthPolicyService {

    private final YouthPolicyClient client; // 외부 API 적재시에만 사용
    private final YouthPolicyRepository repo;
    private final ThreadPoolTaskExecutor youthPolicyFetcherExecutor;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 외부 전체 수집 → DB upsert */
    @Transactional
    public long ingestAll(boolean full) {
        final int fetchChunk = 500;
        int fetchedTotal = 0;
        long t0 = System.currentTimeMillis();

        YouthPolicyApiRequestDto req0 = new YouthPolicyApiRequestDto();
        req0.setPageNum(1);
        req0.setPageSize(fetchChunk);

        YouthPolicyApiResponseDto first = client.search(req0);
        if (first == null || first.getResult() == null) {
            log.warn("[Ingest] first page is null");
            return 0;
        }

        List<Policy> firstItems = nullSafe(first.getResult().getYouthPolicyList());
        upsertPolicies(firstItems, full);
        fetchedTotal += firstItems.size();

        Integer totCount = (first.getResult().getPagging() != null)
                ? first.getResult().getPagging().getTotCount()
                : null;

        if (totCount != null) {
            int totalPages = (int) Math.ceil((double) totCount / fetchChunk);
            if (totalPages > 1) {
                List<CompletableFuture<List<Policy>>> futures = new ArrayList<>();
                for (int p = 2; p <= totalPages; p++) {
                    final int page = p;
                    futures.add(
                            CompletableFuture.<List<Policy>>supplyAsync(() -> {
                                YouthPolicyApiRequestDto r = new YouthPolicyApiRequestDto();
                                r.setPageNum(page);
                                r.setPageSize(fetchChunk);

                                YouthPolicyApiResponseDto resp = client.search(r);
                                List<Policy> items =
                                        (resp != null && resp.getResult() != null)
                                                ? nullSafe(resp.getResult().getYouthPolicyList())
                                                : Collections.<Policy>emptyList();

                                log.debug("[Ingest] fetched page={} size={}", page, items.size());
                                return items;
                            }, youthPolicyFetcherExecutor)
                    );
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                for (CompletableFuture<List<Policy>> f : futures) {
                    List<Policy> items = f.join();
                    upsertPolicies(items, full);
                    fetchedTotal += items.size();
                }
            }
        } else {
            int p = 2, safety = 0;
            while (true) {
                YouthPolicyApiRequestDto r = new YouthPolicyApiRequestDto();
                r.setPageNum(p);
                r.setPageSize(fetchChunk);

                YouthPolicyApiResponseDto resp = client.search(r);
                List<Policy> items =
                        (resp != null && resp.getResult() != null)
                                ? nullSafe(resp.getResult().getYouthPolicyList())
                                : Collections.<Policy>emptyList();

                if (items.isEmpty()) break;
                upsertPolicies(items, full);
                fetchedTotal += items.size();
                if (++safety > 10000) break; // 안전 차단
                p++;
            }
        }

        log.info("[Ingest] Done fetchedTotal={} elapsedMs={}", fetchedTotal, System.currentTimeMillis() - t0);
        return fetchedTotal;
    }

    /**
     * 효율적인 업서트:
     *  - 배치 내 plcyNo 중복 제거
     *  - 기존 엔티티 일괄 조회 → 존재하면 apply(p) (dirty checking으로 UPDATE), 없으면 신규 of(p)만 saveAll
     */
    @Transactional
    protected void upsertPolicies(List<Policy> items, boolean full) {
        if (items == null || items.isEmpty()) return;

        // 1) plcyNo 기준 dedup
        Map<String, Policy> dedup = new LinkedHashMap<>();
        for (Policy p : items) {
            String no = (p != null) ? p.getPlcyNo() : null;
            if (no != null && !no.isBlank()) {
                dedup.put(no, p);
            }
        }
        if (dedup.isEmpty()) return;

        // 2) 존재 여부 벌크 조회
        List<YouthPolicy> existed = repo.findAllByPlcyNoIn(dedup.keySet());
        Map<String, YouthPolicy> existedMap = new HashMap<>(existed.size() * 2);
        for (YouthPolicy e : existed) existedMap.put(e.getPlcyNo(), e);

        // 3) 신규만 수집, 기존은 apply로 갱신
        List<YouthPolicy> toInsert = new ArrayList<>();
        for (Map.Entry<String, Policy> e : dedup.entrySet()) {
            String no = e.getKey();
            Policy p = e.getValue();
            YouthPolicy cur = existedMap.get(no);
            if (cur != null) {
                cur.apply(p); // 영속 상태 → flush 시 UPDATE
            } else {
                toInsert.add(YouthPolicy.of(p)); // 신규 INSERT만 saveAll
            }
        }

        if (!toInsert.isEmpty()) {
            repo.saveAll(toInsert);
        }
    }

    private static <T> List<T> nullSafe(List<T> l) {
        return (l != null) ? l : Collections.<T>emptyList();
    }

    /** DB 기반 검색: recruitingOnly=true면 '모집중'만 메모리 필터 + 서버측 슬라이싱 페이징 */
    @Transactional(readOnly = true)
    public YouthPolicyApiResponseDto searchFromDb(YouthPolicyApiRequestDto req) {
        int pageNum  = (req.getPageNum()  != null && req.getPageNum()  > 0) ? req.getPageNum()  : 1;
        int pageSize = (req.getPageSize() != null && req.getPageSize() > 0) ? req.getPageSize() : 10;
        boolean recruitingOnly = (req.getRecruitingOnly() == null) ? true : req.getRecruitingOnly();

        Specification<YouthPolicy> spec = buildSpec(req);

        List<YouthPolicy> finalList;
        int total;

        if (recruitingOnly) {
            // 1) 전체(필터 조건은 DB에서 적용) 정렬만 주고 수집
            List<YouthPolicy> all = repo.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));

            // 2) 오늘 기준 '모집중'만 필터
            LocalDate today = LocalDate.now();
            List<YouthPolicy> filtered = all.stream()
                    .filter(e -> isRecruitingNow(e.getAplyPrdSeCd(), e.getAplyYmd(), today))
                    .toList();

            // 3) 서버측 슬라이싱 페이징
            total = filtered.size();
            int from = Math.max(0, (pageNum - 1) * pageSize);
            int to   = Math.min(total, from + pageSize);
            finalList = (from < to) ? filtered.subList(from, to) : List.of();

        } else {
            // 기존 DB 페이징
            Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
            Page<YouthPolicy> page = repo.findAll(spec, pageable);
            total = (int) page.getTotalElements();
            finalList = page.getContent();
        }

        List<Policy> list = finalList.stream().map(this::toPolicy).toList();

        Map<String, Object> tree = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pag = new HashMap<>();

        tree.put("resultCode", 0);
        tree.put("resultMessage", "OK");
        tree.put("result", result);
        result.put("youthPolicyList", mapper.convertValue(list, new TypeReference<List<Map<String,Object>>>(){}));
        pag.put("totCount", total);
        pag.put("pageNum", pageNum);
        pag.put("pageSize", pageSize);
        result.put("pagging", pag);

        return mapper.convertValue(tree, YouthPolicyApiResponseDto.class);
    }

    private Specification<YouthPolicy> buildSpec(YouthPolicyApiRequestDto r) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (notBlank(r.getPlcyKywdNm())) {
                ps.add(cb.like(root.get("plcyKywdNm"), "%" + r.getPlcyKywdNm().trim() + "%"));
            }
            if (notBlank(r.getPlcyNm())) {
                ps.add(cb.like(root.get("plcyNm"), "%" + r.getPlcyNm().trim() + "%"));
            }
            if (notBlank(r.getPlcyExpInCn())) {
                ps.add(cb.like(root.get("plcyExplnCn"), "%" + r.getPlcyExpInCn().trim() + "%"));
            }
            if (notBlank(r.getZipCd())) {
                ps.add(cb.like(root.get("zipCd"), "%" + r.getZipCd().trim() + "%"));
            }
            if (notBlank(r.getLclsfNm())) {
                ps.add(cb.like(root.get("lclsfNm"), "%" + r.getLclsfNm().trim() + "%"));
            }
            if (notBlank(r.getMclsfNm())) {
                ps.add(cb.like(root.get("mclsfNm"), "%" + r.getMclsfNm().trim() + "%"));
            }
            if (notBlank(r.getPlcyNo())) {
                ps.add(cb.equal(root.get("plcyNo"), r.getPlcyNo().trim()));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private Policy toPolicy(YouthPolicy e) {
        return Policy.builder()
                .plcyNo(e.getPlcyNo())
                .plcyNm(e.getPlcyNm())
                .plcyKywdNm(e.getPlcyKywdNm())
                .plcyExplnCn(e.getPlcyExplnCn())
                .lclsfNm(e.getLclsfNm())
                .mclsfNm(e.getMclsfNm())
                .plcySprtCn(e.getPlcySprtCn())
                .sprtArvlSeqYn(e.getSprtArvlSeqYn())
                .sprtSclLmtYn(e.getSprtSclLmtYn())
                .sprtSclCnt(e.getSprtSclCnt())
                .aplyPrdSeCd(e.getAplyPrdSeCd())
                .aplyYmd(e.getAplyYmd())
                .aplyUrlAddr(e.getAplyUrlAddr())
                .plcyAplyMthdCn(e.getPlcyAplyMthdCn())
                .sprvsnInstCdNm(e.getSprvsnInstCdNm())
                .operInstCdNm(e.getOperInstCdNm())
                .sprtTrgtMinAge(e.getSprtTrgtMinAge())
                .sprtTrgtMaxAge(e.getSprtTrgtMaxAge())
                .sprtTrgtAgeLmtYn(e.getSprtTrgtAgeLmtYn())
                .mrgSttsCd(e.getMrgSttsCd())
                .earnCndSeCd(e.getEarnCndSeCd())
                .earnMinAmt(e.getEarnMinAmt())
                .earnMaxAmt(e.getEarnMaxAmt())
                .earnEtcCn(e.getEarnEtcCn())
                .jobCd(e.getJobCd())
                .plcyMajorCd(e.getPlcyMajorCd())
                .schoolCd(e.getSchoolCd())
                .sbizCd(e.getSbizCd())
                .addAplyQlfcCndCn(e.getAddAplyQlfcCndCn())
                .ptcpPrpTrgtCn(e.getPtcpPrpTrgtCn())
                .srngMthdCn(e.getSrngMthdCn())
                .sbmsnDcmntCn(e.getSbmsnDcmntCn())
                .etcMttrCn(e.getEtcMttrCn())
                .refUrlAddr1(e.getRefUrlAddr1())
                .refUrlAddr2(e.getRefUrlAddr2())
                .zipCd(e.getZipCd())
                .inqCnt(e.getInqCnt())
                .frstRegDt(e.getFrstRegDt())
                .lastMdfcnDt(e.getLastMdfcnDt())
                .build();
    }

    @Transactional(readOnly = true)
    public YouthPolicyDetailDto detailFromDb(String plcyNo) {
        YouthPolicy e = repo.findByPlcyNo(plcyNo)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다: " + plcyNo));
        Policy p = toPolicy(e);
        return YouthPolicyMapper.toDetail(p);
    }

    // ====== 모집중 판정 유틸 ======

    private static final String APPLY_PERIOD_SPECIFIC = "0057001"; // 특정기간
    private static final String APPLY_PERIOD_ALWAYS   = "0057002"; // 상시
    private static final String APPLY_PERIOD_CLOSED   = "0057003"; // 마감

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern DATE_TOKEN = Pattern.compile("(\\d{4}[\\.\\-/]?\\d{2}[\\.\\-/]?\\d{2})");

    /** 오늘 기준 모집 여부 */
    static boolean isRecruitingNow(String aplyPrdSeCd, String aplyYmd, LocalDate today) {
        if (APPLY_PERIOD_ALWAYS.equals(aplyPrdSeCd)) return true;      // 상시
        if (APPLY_PERIOD_CLOSED.equals(aplyPrdSeCd)) return false;     // 마감
        if (!APPLY_PERIOD_SPECIFIC.equals(aplyPrdSeCd)) return false;  // 그 외/비정상 ⇒ 모집 아님

        List<DateRange> ranges = parseRangesFrom(aplyYmd);
        if (ranges.isEmpty()) return false;
        for (DateRange r : ranges) {
            if (r != null && r.start != null && r.end != null) {
                if (!today.isBefore(r.start) && !today.isAfter(r.end)) return true; // start <= today <= end
            }
        }
        return false;
    }

    /** "YYYYMMDD ~ YYYYMMDD" 가 여러 줄/\\N로 이어질 수 있음 → 모든 구간을 뽑아냄 */
    static List<DateRange> parseRangesFrom(String raw) {
        String v = raw == null ? "" : raw.replace("\\N", "\n");
        if (v.isBlank()) return List.of();

        List<DateRange> out = new ArrayList<>();
        String[] lines = v.split("\\r?\\n");
        for (String line : lines) {
            Matcher m = DATE_TOKEN.matcher(line);
            LocalDate s = null, e = null;
            if (m.find()) s = parseFlexible(m.group(1));
            if (m.find()) e = parseFlexible(m.group(1));
            if (s != null && e != null && !e.isBefore(s)) {
                out.add(new DateRange(s, e));
            }
        }
        return out;
    }

    static LocalDate parseFlexible(String token) {
        if (token == null || token.isBlank()) return null;
        String digits = token.replaceAll("\\D", "");
        if (digits.length() < 8) return null;
        String ymd8 = digits.substring(0, 8);
        try {
            return LocalDate.parse(ymd8, YYYYMMDD);
        } catch (Exception e) {
            return null;
        }
    }

    static final class DateRange {
        final LocalDate start;
        final LocalDate end;
        DateRange(LocalDate s, LocalDate e){ this.start = s; this.end = e; }
    }
}
