package com.youthjob.api.empprogram.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.youthjob.api.empprogram.client.EmpProgramApiClient;
import com.youthjob.api.empprogram.domain.SavedEmpProgram;
import com.youthjob.api.empprogram.dto.EmpProgramResponseDto;
import com.youthjob.api.empprogram.dto.SaveEmpProgramRequest;
import com.youthjob.api.empprogram.dto.SavedEmpProgramDto;
import com.youthjob.api.empprogram.repository.SavedEmpProgramRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class EmpProgramService {

    private final EmpProgramApiClient client;
    private final SavedEmpProgramRepository repo;

    private static final XmlMapper XML = new XmlMapper();

    public EmpProgramResponseDto search(String pgmStdt, String topOrgCd, String orgCd,
                                        Integer startPage, Integer display) {
        String xml = client.getProgramsXml(pgmStdt, topOrgCd, orgCd, startPage, display);

        try {
            return XML.readValue(xml, EmpProgramResponseDto.class);

        } catch (Exception e) {
            return EmpProgramResponseDto.builder()
                    .total(0)
                    .startPage(startPage == null ? 1 : startPage)
                    .display(display == null ? 10 : display)
                    .message("XML parse error: " + e.getMessage())
                    .messageCd("PARSE_ERROR")
                    .programs(List.of())
                    .build();
        }
    }

    /** 멤버가 같은 항목을 여러 번 저장하지 않도록 외부키를 합성 */
    static String buildExtKey(SaveEmpProgramRequest r) {
        // 충돌 방지를 위해 구분자 포함. 필요하면 해시로 바꿔도 됨.
        return String.join("|",
                nv(r.getOrgNm()), nv(r.getPgmNm()), nv(r.getPgmSubNm()),
                nv(r.getPgmStdt()), nv(r.getPgmEndt()), nv(r.getOpenTimeClcd()), nv(r.getOpenTime()));
    }
    private static String nv(String s) { return s == null ? "" : s; }

    @Transactional
    public SavedEmpProgramDto save(Long memberId, SaveEmpProgramRequest r) {
        String extKey = buildExtKey(r);

        // 이미 저장되어 있으면 그걸 반환(또는 409로 막고 싶으면 예외로 처리)
        return repo.findByMemberIdAndExtKey(memberId, extKey)
                .map(SavedEmpProgramDto::from)
                .orElseGet(() -> {
                    SavedEmpProgram saved = repo.save(
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

    public List<SavedEmpProgramDto> list(Long memberId) {
        return repo.findByMemberIdOrderByIdDesc(memberId)
                .stream().map(SavedEmpProgramDto::from).collect(toList());
    }

    @Transactional
    public void delete(Long memberId, Long id) {
        SavedEmpProgram e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!e.getMemberId().equals(memberId)) {
            throw new SecurityException("NOT_OWNER");
        }
        repo.delete(e);
    }

}
