package com.youthjob.api.youthpolicy.dto;

import com.youthjob.api.youthpolicy.domain.SavedPolicy;

public record SavedPolicyDto(
        Long id,
        String plcyNo,
        String plcyNm,
        String plcyKywdNm,
        String lclsfNm,
        String mclsfNm,
        String aplyYmd,
        String aplyUrlAddr,
        String sprvsnInstCdNm,
        String operInstCdNm
) {
    public static SavedPolicyDto from(SavedPolicy s) {
        return new SavedPolicyDto(
                s.getId(), s.getPlcyNo(), s.getPlcyNm(), s.getPlcyKywdNm(),
                s.getLclsfNm(), s.getMclsfNm(), s.getAplyYmd(), s.getAplyUrlAddr(),
                s.getSprvsnInstCdNm(), s.getOperInstCdNm()
        );
    }
}
