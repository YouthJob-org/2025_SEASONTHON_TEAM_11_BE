package com.youthjob.api.youthpolicy.service;

import java.util.*;

final class CodeDict {
    private CodeDict(){}

    static final Map<String,String> PVSN_INST_GROUP = Map.of(
            "0054001","중앙부처", "0054002","지자체"
    );

    static final Map<String,String> PVSN_METHOD = Map.ofEntries(
            Map.entry("0042001","인프라 구축"), Map.entry("0042002","프로그램"),
            Map.entry("0042003","직접대출"),   Map.entry("0042004","공공기관"),
            Map.entry("0042005","계약(위탁운영)"), Map.entry("0042006","보조금"),
            Map.entry("0042007","대출보증"),   Map.entry("0042008","공적보험"),
            Map.entry("0042009","조세지출"),   Map.entry("0042010","바우처"),
            Map.entry("0042011","정보제공"),   Map.entry("0042012","경제적 규제"),
            Map.entry("0042013","기타")
    );

    static final Map<String,String> APPLY_PERIOD = Map.of(
            "0057001","특정기간","0057002","상시","0057003","마감"
    );

    static final Map<String,String> BIZ_PERIOD = Map.of(
            "0056001","특정기간","0056002","기타"
    );

    static final Map<String,String> MARITAL = Map.of(
            "0055001","기혼","0055002","미혼","0055003","제한없음"
    );

    static final Map<String,String> INCOME = Map.of(
            "0043001","무관","0043002","연소득","0043003","기타"
    );

    static final Map<String,String> MAJOR = Map.ofEntries(
            Map.entry("0011001","인문계열"), Map.entry("0011002","사회계열"),
            Map.entry("0011003","상경계열"), Map.entry("0011004","이학계열"),
            Map.entry("0011005","공학계열"), Map.entry("0011006","예체능계열"),
            Map.entry("0011007","농산업계열"), Map.entry("0011008","기타"),
            Map.entry("0011009","제한없음")
    );

    static final Map<String,String> JOB = Map.ofEntries(
            Map.entry("0013001","재직자"), Map.entry("0013002","자영업자"),
            Map.entry("0013003","미취업자"), Map.entry("0013004","프리랜서"),
            Map.entry("0013005","일용근로자"), Map.entry("0013006","(예비)창업자"),
            Map.entry("0013007","단기근로자"), Map.entry("0013008","영농종사자"),
            Map.entry("0013009","기타"),     Map.entry("0013010","제한없음")
    );

    static final Map<String,String> SCHOOL = Map.ofEntries(
            Map.entry("0049001","고졸 미만"), Map.entry("0049002","고교 재학"),
            Map.entry("0049003","고졸 예정"), Map.entry("0049004","고교 졸업"),
            Map.entry("0049005","대학 재학"), Map.entry("0049006","대졸 예정"),
            Map.entry("0049007","대학 졸업"), Map.entry("0049008","석·박사"),
            Map.entry("0049009","기타"),     Map.entry("0049010","제한없음")
    );

    static final Map<String,String> SBIZ = Map.ofEntries(
            Map.entry("0014001","중소기업"), Map.entry("0014002","여성"),
            Map.entry("0014003","기초생활수급자"), Map.entry("0014004","한부모가정"),
            Map.entry("0014005","장애인"), Map.entry("0014006","농업인"),
            Map.entry("0014007","군인"), Map.entry("0014008","지역인재"),
            Map.entry("0014009","기타"), Map.entry("0014010","제한없음")
    );

    static String label(Map<String,String> dict, String code){
        if (code==null || code.isBlank()) return null;
        return dict.getOrDefault(code.trim(), code.trim());
    }

    static java.util.List<String> decodeCsv(Map<String,String> dict, String csv){
        if (csv==null || csv.isBlank()) return java.util.List.of();
        String[] parts = csv.split("\\s*,\\s*");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p: parts) {
            if (!p.isBlank()) out.add(label(dict, p));
        }
        return out;
    }
}
