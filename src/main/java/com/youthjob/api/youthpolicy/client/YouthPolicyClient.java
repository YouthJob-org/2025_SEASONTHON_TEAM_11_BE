package com.youthjob.api.youthpolicy.client;

import com.youthjob.api.youthpolicy.dto.YouthPolicyApiRequestDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouthPolicyClient {

    private final RestClient youthPolicyRestClient;

    @Value("${api.youthpolicy.key}")
    private String apiKey;

    @Value("${api.youthpolicy.path:/go/ythip/getPlcy}")
    private String path;

    public YouthPolicyApiResponseDto search(YouthPolicyApiRequestDto req) {
        var params = req.toQueryParams(apiKey);

        var resp = youthPolicyRestClient.get()
                .uri(u -> u.path(path).queryParams(params).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(YouthPolicyApiResponseDto.class);

        if (log.isDebugEnabled()) {
            log.debug("[YouthPolicy] pageNum={}, pageSize={}, pageType={}, kywd={}, name={}, zip={}, l={}, m={}",
                    req.getPageNum(), req.getPageSize(), req.getPageType(),
                    req.getPlcyKywdNm(), req.getPlcyNm(), req.getZipCd(), req.getLclsfNm(), req.getMclsfNm());
        }
        return resp;
    }

    public YouthPolicyApiResponseDto findByPlcyNo(String plcyNo) {
        var p = new org.springframework.util.LinkedMultiValueMap<String, String>();
        p.add("apiKeyNm", apiKey);
        p.add("rtnType", "json");
        p.add("pageNum", "1");
        p.add("pageSize", "1");
        p.add("pageType", "2");
        p.add("plcyNo", plcyNo);

        return youthPolicyRestClient.get()
                .uri(u -> u.path(path).queryParams(p).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(YouthPolicyApiResponseDto.class);
    }
}
