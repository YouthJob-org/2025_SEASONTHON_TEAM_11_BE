package com.youthjob.api.youthpolicy.service;

import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.api.youthpolicy.domain.SavedPolicy;
import com.youthjob.api.youthpolicy.dto.SavePolicyRequest;
import com.youthjob.api.youthpolicy.dto.SavedPolicyDto;
import com.youthjob.api.youthpolicy.dto.YouthPolicyApiResponseDto;
import com.youthjob.api.youthpolicy.client.YouthPolicyClient;
import com.youthjob.api.youthpolicy.repository.SavedPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YouthPolicyFavoriteService {

    private final SavedPolicyRepository savedPolicyRepository;
    private final YouthPolicyClient client;
    private final UserRepository userRepository;

    public List<SavedPolicyDto> listSaved() {
        User me = currentUser();
        return savedPolicyRepository.findAllByUserOrderByCreatedAtDesc(me)
                .stream().map(SavedPolicyDto::from).toList();
    }

    @Transactional
    public SavedPolicyDto save(SavePolicyRequest req) {
        User me = currentUser();

        // 이미 있으면 그대로 반환
        var existed = savedPolicyRepository.findByUserAndPlcyNo(me, req.plcyNo());
        if (existed.isPresent()) return SavedPolicyDto.from(existed.get());

        // 스냅샷이 충분하면 그대로, 아니면 외부 API에서 채움
        Snapshot snap = snapshotOrFetch(req);

        SavedPolicy saved = new SavedPolicy(
                me,
                req.plcyNo(),
                snap.plcyNm, snap.plcyKywdNm,
                snap.lclsfNm, snap.mclsfNm,
                snap.aplyYmd, snap.aplyUrlAddr,
                snap.sprvsnInstCdNm, snap.operInstCdNm
        );
        return SavedPolicyDto.from(savedPolicyRepository.save(saved));
    }

    @Transactional
    public void delete(Long id) {
        User me = currentUser();
        var target = savedPolicyRepository.findByIdAndUser(id, me)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 항목을 찾을 수 없습니다."));
        savedPolicyRepository.delete(target);
    }

    /** 없으면 저장, 있으면 삭제하고 null 반환 (HRD 스타일) */
    @Transactional
    public SavedPolicyDto toggle(SavePolicyRequest req) {
        User me = currentUser();
        var existed = savedPolicyRepository.findByUserAndPlcyNo(me, req.plcyNo());
        if (existed.isPresent()) {
            savedPolicyRepository.delete(existed.get());
            return null;
        }
        return save(req);
    }

    // ===== 내부 유틸 =====
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + auth.getName()));
    }

    private record Snapshot(
            String plcyNm, String plcyKywdNm,
            String lclsfNm, String mclsfNm,
            String aplyYmd, String aplyUrlAddr,
            String sprvsnInstCdNm, String operInstCdNm
    ) {}

    /** 요청에 스냅샷이 충분하면 그대로, 아니면 plcyNo로 외부API 상세 조회해서 채움 */
    private Snapshot snapshotOrFetch(SavePolicyRequest req) {
        boolean hasSnapshot =
                notBlank(req.plcyNm()) || notBlank(req.plcyKywdNm()) ||
                notBlank(req.lclsfNm()) || notBlank(req.mclsfNm()) ||
                notBlank(req.aplyYmd()) || notBlank(req.aplyUrlAddr()) ||
                notBlank(req.sprvsnInstCdNm()) || notBlank(req.operInstCdNm());

        if (hasSnapshot) {
            return new Snapshot(
                    req.plcyNm(), req.plcyKywdNm(),
                    req.lclsfNm(), req.mclsfNm(),
                    req.aplyYmd(), req.aplyUrlAddr(),
                    req.sprvsnInstCdNm(), req.operInstCdNm()
            );
        }

        // 외부 API 상세 조회 (pageType=2)
        YouthPolicyApiResponseDto resp = client.findByPlcyNo(req.plcyNo());
        var list = resp != null && resp.getResult() != null ? resp.getResult().getYouthPolicyList() : null;
        var p = (list != null && !list.isEmpty()) ? list.get(0) : null;
        if (p == null) throw new IllegalStateException("plcyNo 상세를 찾을 수 없습니다: " + req.plcyNo());

        return new Snapshot(
                p.getPlcyNm(), p.getPlcyKywdNm(),
                p.getLclsfNm(), p.getMclsfNm(),
                p.getAplyYmd(), p.getAplyUrlAddr(),
                p.getSprvsnInstCdNm(), p.getOperInstCdNm()
        );
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
