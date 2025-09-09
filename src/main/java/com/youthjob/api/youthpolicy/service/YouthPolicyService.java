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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

        YouthPolicyApiRequestDto req0 = new YouthPolicyApiRequestDto();
        req0.setPageNum(1);
        req0.setPageSize(fetchChunk);
        YouthPolicyApiResponseDto first = client.search(req0);
        if (first == null || first.getResult() == null) return 0;

        List<Policy> firstItems = nullSafe(first.getResult().getYouthPolicyList());
        upsertPolicies(firstItems, full);
        fetchedTotal += firstItems.size();

        Integer totCount = first.getResult().getPagging() != null
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
                if (++safety > 10000) break;
                p++;
            }
        }

        log.info("[Ingest] Done. fetchedTotal={}", fetchedTotal);
        return fetchedTotal;
    }

    /** Upsert: plcyNo 기준 중복 제거 후 있으면 apply(p), 없으면 of(p) */
    @Transactional
    protected void upsertPolicies(List<Policy> items, boolean full) {
        if (items == null || items.isEmpty()) return;

        // 1) 같은 배치 내 plcyNo 중복 제거 (마지막 값으로 덮어쓰기)
        Map<String, Policy> dedup = new LinkedHashMap<>();
        for (Policy p : items) {
            if (p.getPlcyNo() != null && !p.getPlcyNo().isBlank()) {
                dedup.put(p.getPlcyNo(), p);
            }
        }
        if (dedup.isEmpty()) return;

        // 2) 기존 엔티티 일괄 조회 → 맵
        List<YouthPolicy> existing = repo.findAllByPlcyNoIn(dedup.keySet());
        Map<String, YouthPolicy> map = new HashMap<>(existing.size());
        for (YouthPolicy e : existing) map.put(e.getPlcyNo(), e);

        // 3) upsert
        List<YouthPolicy> toSave = new ArrayList<>(dedup.size());
        for (Map.Entry<String, Policy> e : dedup.entrySet()) {
            String no = e.getKey();
            Policy p = e.getValue();
            YouthPolicy cur = map.get(no);
            if (cur != null) {
                cur.apply(p);
                toSave.add(cur);
            } else {
                toSave.add(YouthPolicy.of(p));
            }
        }

        repo.saveAll(toSave);
    }

    private static <T> List<T> nullSafe(List<T> l){
        return (l != null) ? l : Collections.<T>emptyList();
    }

    /** DB 기반 검색 (외부 API 호출 없음, 기존 응답 스펙 유지) */
    @Transactional(readOnly = true)
    public YouthPolicyApiResponseDto searchFromDb(YouthPolicyApiRequestDto req) {
        int pageNum  = (req.getPageNum()  != null && req.getPageNum()  > 0) ? req.getPageNum()  : 1;
        int pageSize = (req.getPageSize() != null && req.getPageSize() > 0) ? req.getPageSize() : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Specification<YouthPolicy> spec = buildSpec(req);

        Page<YouthPolicy> page = repo.findAll(spec, pageable);
        List<Policy> list = page.getContent().stream().map(this::toPolicy).toList();

        Map<String, Object> tree = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pag = new HashMap<>();

        tree.put("resultCode", 0);
        tree.put("resultMessage", "OK");
        tree.put("result", result);
        result.put("youthPolicyList", mapper.convertValue(list, new TypeReference<List<Map<String,Object>>>(){}));
        pag.put("totCount", (int) page.getTotalElements());
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

    private boolean notBlank(String s){ return s != null && !s.isBlank(); }

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
}
