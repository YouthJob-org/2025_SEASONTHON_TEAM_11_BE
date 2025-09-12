package com.youthjob.api.empprogram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "emp.centers")
public class EmpCentersProps {

    private String topOrgCds;
    private Map<String, String> byTop = Collections.emptyMap();

    public List<String> topOrgCdList() {
        if (topOrgCds == null || topOrgCds.isBlank()) return List.of();
        return topOrgCds.replace(" ", "").split(",").length == 0
                ? List.of()
                : List.of(topOrgCds.replace(" ", "").split(","));
    }
}
