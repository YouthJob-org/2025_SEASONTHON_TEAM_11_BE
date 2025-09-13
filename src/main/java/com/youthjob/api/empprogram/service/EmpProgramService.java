package com.youthjob.api.empprogram.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.youthjob.api.empprogram.client.EmpProgramApiClient;
import com.youthjob.api.empprogram.domain.EmpProgramCatalog;
import com.youthjob.api.empprogram.domain.SavedEmpProgram;
import com.youthjob.api.empprogram.dto.EmpProgramItemDto;
import com.youthjob.api.empprogram.dto.EmpProgramResponseDto;
import com.youthjob.api.empprogram.dto.SaveEmpProgramRequest;
import com.youthjob.api.empprogram.dto.SavedEmpProgramDto;
import com.youthjob.api.empprogram.repository.EmpProgramCatalogRepository;
import com.youthjob.api.empprogram.repository.SavedEmpProgramRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;   // ✔ spring-data
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate; // ✔ JPA Criteria
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class EmpProgramService {

    private final EmpProgramApiClient client; // (다른 곳에서 쓰면 유지, 아니면 제거 가능)
    private final SavedEmpProgramRepository savedRepo;   // 저장목록
    private final EmpProgramCatalogRepository catalogRepo; // ✔ 카탈로그(DB 검색용)

    private static final XmlMapper XML = new XmlMapper();

    /** DB에서 검색해서 외부 API와 동일 DTO로 반환 */
    public EmpProgramResponseDto search(String pgmStdt, String topOrgCd, String orgCd,
                                        Integer startPage, Integer display) {
        // pgmStdt 미입력 시 오늘(KST)
        String day = (pgmStdt == null || pgmStdt.isBlank())
                ? LocalDate.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : pgmStdt;

        int page = (startPage == null || startPage < 1) ? 1 : Math.min(startPage, 1000);
        int size = (display   == null || display   < 1) ? 10 : Math.min(display, 100);

        // 동적 조건 (JPA Criteria Predicate 사용)
        Specification<EmpProgramCatalog> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("pgmStdt"), day));
            if (topOrgCd != null && !topOrgCd.isBlank()) {
                preds.add(cb.equal(root.get("topOrgCd"), topOrgCd));
            }
            if (orgCd != null && !orgCd.isBlank()) {
                preds.add(cb.equal(root.get("orgCd"), orgCd));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };

        // 정렬: 시작일 오름차순 -> id 오름차순
        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Order.asc("pgmStdt"), Sort.Order.asc("id")));

        Page<EmpProgramCatalog> result = catalogRepo.findAll(spec, pageable);

        // 매핑: EmpProgramCatalog -> EmpProgramItemDto
        List<EmpProgramItemDto> items = result.getContent().stream()
                .map(e -> EmpProgramItemDto.builder()
                        .orgNm(e.getOrgNm())
                        .pgmNm(e.getPgmNm())
                        .pgmSubNm(e.getPgmSubNm())
                        .pgmTarget(e.getPgmTarget())
                        .pgmStdt(e.getPgmStdt())
                        .pgmEndt(e.getPgmEndt())
                        .openTimeClcd(e.getOpenTimeClcd())
                        .openTime(e.getOpenTime())
                        .operationTime(e.getOperationTime())
                        .openPlcCont(e.getOpenPlcCont())
                        .build())
                .toList();

        return EmpProgramResponseDto.builder()
                .total((int) result.getTotalElements())
                .startPage(page)
                .display(size)
                .programs(items)
                .build();
    }

    /** 저장목록 키 생성 */
    static String buildExtKey(SaveEmpProgramRequest r) {
        return String.join("|",
                nv(r.getOrgNm()), nv(r.getPgmNm()), nv(r.getPgmSubNm()),
                nv(r.getPgmStdt()), nv(r.getPgmEndt()), nv(r.getOpenTimeClcd()), nv(r.getOpenTime()));
    }
    private static String nv(String s) { return s == null ? "" : s; }

    /** 저장 */
    @Transactional
    public SavedEmpProgramDto save(Long memberId, SaveEmpProgramRequest r) {
        String extKey = buildExtKey(r);

        return savedRepo.findByMemberIdAndExtKey(memberId, extKey)
                .map(SavedEmpProgramDto::from)
                .orElseGet(() -> {
                    SavedEmpProgram saved = savedRepo.save(
                            SavedEmpProgram.builder()
                                    .memberId(memberId)
                                    .extKey(extKey)
                                    .orgNm(r.getOrgNm())
                                    .pgmNm(r.getPgmNm())
                                    .pgmSubNm(r.getPgmSubNm())
                                    .pgmTarget(r.getPgmTarget())
                                    .pgmStdt(r.getPgmStdt())
                                    .pgmEndt(r.getPgmEndt())
                                    .openTimeClcd(r.getOpenTimeClcd())
                                    .openTime(r.getOpenTime())
                                    .operationTime(r.getOperationTime())
                                    .openPlcCont(r.getOpenPlcCont())
                                    .build()
                    );
                    return SavedEmpProgramDto.from(saved);
                });
    }

    /** 저장목록 조회 */
    public List<SavedEmpProgramDto> list(Long memberId) {
        return savedRepo.findByMemberIdOrderByIdDesc(memberId)
                .stream().map(SavedEmpProgramDto::from).collect(toList());
    }

    /** 저장목록 삭제 */
    @Transactional
    public void delete(Long memberId, Long id) {
        SavedEmpProgram e = savedRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!e.getMemberId().equals(memberId)) {
            throw new SecurityException("NOT_OWNER");
        }
        savedRepo.delete(e);
    }
}
