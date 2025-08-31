package com.youthjob.api.youthpolicy.client;

import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class YouthPolicyClient {

    private final RestClient youthPolicyRestClient;

    @Value("${api.youthpolicy.key}")
    private String apiKey;

    @Value("${api.youthpolicy.path:/go/ythip/getPlcy}")
    private String path;

    public YouthPolicyApiResponseDto search(YouthPolicyApiRequestDto req) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // 필수 파라미터
        params.add("apiKeyNm", apiKey);
        params.add("rtnType",  req.getRtnType());
        params.add("pageNum",  String.valueOf(req.getPageNum()));
        params.add("pageSize", String.valueOf(req.getPageSize()));
        params.add("pageType", String.valueOf(req.getPageType()));
        // 선택 파라미터
        add(params, "plcyNo",      req.getPlcyNo());
        add(params, "plcyKywdNm",  req.getPlcyKywdNm());
        add(params, "plcyExpInCn", req.getPlcyExpInCn());
        add(params, "plcyNm",      req.getPlcyNm());
        add(params, "zipCd",       req.getZipCd());
        add(params, "lclsfNm",     req.getLclsfNm());
        add(params, "mclsfNm",     req.getMclsfNm());

        return youthPolicyRestClient.get()
                .uri(uri -> uri.path(path).queryParams(params).build())
                .retrieve()
                .body(YouthPolicyApiResponseDto.class);
    }

    // 스냅샷이 없는 경우 외부 api를 직접 호출하는 방식 -> 호출 후 저장함
    public YouthPolicyApiResponseDto findByPlcyNo(String plcyNo) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("apiKeyNm", apiKey);
        params.add("rtnType", "json");
        params.add("pageNum", "1");
        params.add("pageSize", "1");
        params.add("pageType", "2");      // 상세
        params.add("plcyNo", plcyNo);

        return youthPolicyRestClient.get()
                .uri(u -> u.path(path).queryParams(params).build())
                .retrieve()
                .body(YouthPolicyApiResponseDto.class);
    }


    private static void add(MultiValueMap<String, String> map, String k, String v) {
        if (v != null && !v.isBlank()) map.add(k, v);
    }
}
