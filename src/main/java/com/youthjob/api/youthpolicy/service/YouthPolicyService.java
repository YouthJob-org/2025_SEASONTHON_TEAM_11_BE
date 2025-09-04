package com.youthjob.api.youthpolicy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouthPolicyService {

    private final YouthPolicyClient client;
    private final ThreadPoolTaskExecutor youthPolicyFetcherExecutor;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * - 병렬 수집 + 파라미터 기반 캐시 + 서버단 페이징
     * - 캐시 키: 필터 + pageNum + pageSize
     * - 외부 API는 끝까지 병렬 수집
     * - 수집 후 서버단 필터 → (totCount 계산) → 요청한 pageNum/pageSize로 슬라이스
     */
    @Cacheable(
            value = "youthPolicies",
            key =
                    "'yp:'"
                            + " + (#req.plcyKywdNm != null ? #req.plcyKywdNm : '')"
                            + " + ':' + (#req.plcyNm != null ? #req.plcyNm : '')"
                            + " + ':' + (#req.plcyExpInCn != null ? #req.plcyExpInCn : '')"
                            + " + ':' + (#req.zipCd != null ? #req.zipCd : '')"
                            + " + ':' + (#req.lclsfNm != null ? #req.lclsfNm : '')"
                            + " + ':' + (#req.mclsfNm != null ? #req.mclsfNm : '')"
                            + " + ':' + (#req.pageNum != null ? #req.pageNum : 1)"
                            + " + ':' + (#req.pageSize != null ? #req.pageSize : 10)"
    )

    public YouthPolicyApiResponseDto searchWithFilter(YouthPolicyApiRequestDto req) {
        long t0 = System.currentTimeMillis();

        final int fetchChunk = 500;
        final int startPage  = (req.getPageNum()  != null && req.getPageNum()  > 0) ? req.getPageNum()  : 1;
        final int pageSize   = (req.getPageSize() != null && req.getPageSize() > 0) ? req.getPageSize() : 10;

        YouthPolicyApiResponseDto first = client.search(copyForPage(req, startPage, fetchChunk));
        if (first == null || first.getResult() == null) return first;

        List<Policy> acc = new ArrayList<>(nullSafe(first.getResult().getYouthPolicyList()));
        Integer totCount = (first.getResult().getPagging() != null)
                ? first.getResult().getPagging().getTotCount()
                : null;

        if (totCount != null) {
            int totalPages = (int) Math.ceil((double) totCount / Math.max(fetchChunk, 1));
            if (totalPages > startPage) {
                List<CompletableFuture<List<Policy>>> futures = new ArrayList<>();
                for (int p = startPage + 1; p <= totalPages; p++) {
                    final int page = p;
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        YouthPolicyApiResponseDto r = client.search(copyForPage(req, page, fetchChunk));
                        List<Policy> items = (r != null && r.getResult() != null)
                                ? nullSafe(r.getResult().getYouthPolicyList())
                                : List.of();
                        log.debug("[YouthPolicy] fetched page={} chunk={} count={}", page, fetchChunk, items.size());
                        return items;
                    }, youthPolicyFetcherExecutor));
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                for (int i = 0; i < futures.size(); i++) {
                    try { acc.addAll(futures.get(i).join()); }
                    catch (Exception e) {
                        int failPage = startPage + 1 + i;
                        log.warn("[YouthPolicy] page {} join failed: {}", failPage, e.toString());
                    }
                }
            }
        } else {
            int p = startPage + 1, safety = 0;
            while (true) {
                YouthPolicyApiResponseDto r = client.search(copyForPage(req, p, fetchChunk));
                List<Policy> items = (r != null && r.getResult() != null)
                        ? nullSafe(r.getResult().getYouthPolicyList())
                        : List.of();
                if (items.isEmpty()) break;
                acc.addAll(items);
                if (items.size() < fetchChunk) break;
                if (++safety > 5000) { log.warn("[YouthPolicy] safety break at page {}", p); break; }
                p++;
            }
        }

        List<Policy> filtered = applyServerFilters(acc, req);

        int total = filtered.size();
        int from  = Math.max(0, (startPage - 1) * pageSize);
        int to    = Math.min(total, from + pageSize);
        List<Policy> slice = (from < to) ? filtered.subList(from, to) : List.of();

        Map<String, Object> tree   = mapper.convertValue(first, new TypeReference<>() {});
        Map<String, Object> result = castMap(tree.get("result"));
        Map<String, Object> pag    = castMap(result.get("pagging"));

        result.put("youthPolicyList",
                mapper.convertValue(slice, new TypeReference<List<Map<String, Object>>>() {}));
        pag.put("totCount", total);
        pag.put("pageNum",  startPage);
        pag.put("pageSize", pageSize);

        log.info("[YouthPolicy] OK filters={}, total={}, page={}/{}, elapsedMs={}ms",
                filterKeyForLog(req), total, startPage, pageSize, System.currentTimeMillis() - t0);

        return mapper.convertValue(tree, YouthPolicyApiResponseDto.class);
    }

    // ===== 유틸 =====
    private static YouthPolicyApiRequestDto copyForPage(YouthPolicyApiRequestDto o, int pageNum, int pageSize) {
        YouthPolicyApiRequestDto c = new YouthPolicyApiRequestDto();
        c.setRtnType(o.getRtnType());
        c.setPageType(o.getPageType());
        c.setPageNum(pageNum);
        c.setPageSize(pageSize);
        c.setPlcyNo(o.getPlcyNo());
        c.setPlcyKywdNm(o.getPlcyKywdNm());
        c.setPlcyNm(o.getPlcyNm());
        c.setPlcyExpInCn(o.getPlcyExpInCn());
        c.setZipCd(o.getZipCd());
        c.setLclsfNm(o.getLclsfNm());
        c.setMclsfNm(o.getMclsfNm());
        return c;
    }

    private static <T> List<T> nullSafe(List<T> l){ return l != null ? l : List.of(); }
    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }
    private static boolean contains(String src, String kw){ return src != null && src.contains(kw); }
    private static boolean containsAnyToken(String src, String kw){
        if (src == null || kw == null || kw.isBlank()) return false;
        return Stream.of(src.split("[,，]")).map(String::trim).anyMatch(t -> t.contains(kw));
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> castMap(Object o){
        return (o instanceof Map) ? (Map<String, Object>) o : new HashMap<>();
    }

    private static String filterKeyForLog(YouthPolicyApiRequestDto r) {
        return String.join("|",
                Optional.ofNullable(r.getPlcyKywdNm()).orElse(""),
                Optional.ofNullable(r.getPlcyNm()).orElse(""),
                Optional.ofNullable(r.getPlcyExpInCn()).orElse(""),
                Optional.ofNullable(r.getZipCd()).orElse(""),
                Optional.ofNullable(r.getLclsfNm()).orElse(""),
                Optional.ofNullable(r.getMclsfNm()).orElse(""));
    }

    /** 서버단 필터 */
    private List<Policy> applyServerFilters(List<Policy> src, YouthPolicyApiRequestDto req) {
        List<Policy> list = new ArrayList<>(src);
        if (notBlank(req.getPlcyKywdNm())) {
            String kw = req.getPlcyKywdNm().trim();
            list = list.stream().filter(p -> containsAnyToken(p.getPlcyKywdNm(), kw)).toList();
        }
        if (notBlank(req.getPlcyNm())) {
            String kw = req.getPlcyNm().trim();
            list = list.stream().filter(p -> contains(p.getPlcyNm(), kw)).toList();
        }
        if (notBlank(req.getPlcyExpInCn())) {
            String kw = req.getPlcyExpInCn().trim();
            list = list.stream().filter(p -> contains(p.getPlcyExplnCn(), kw)).toList();
        }
        if (notBlank(req.getZipCd())) {
            String kw = req.getZipCd().trim();
            list = list.stream().filter(p -> contains(p.getZipCd(), kw)).toList();
        }
        if (notBlank(req.getLclsfNm())) {
            String kw = req.getLclsfNm().trim();
            list = list.stream().filter(p -> contains(p.getLclsfNm(), kw)).toList();
        }
        if (notBlank(req.getMclsfNm())) {
            String kw = req.getMclsfNm().trim();
            list = list.stream().filter(p -> contains(p.getMclsfNm(), kw)).toList();
        }
        return list;
    }
}
