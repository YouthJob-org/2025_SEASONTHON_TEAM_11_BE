package com.youthjob.api.youthpolicy.service;

import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.api.youthpolicy.domain.SavedPolicy;
import com.youthjob.api.youthpolicy.domain.YouthPolicy;
import com.youthjob.api.youthpolicy.dto.SavePolicyRequest;
import com.youthjob.api.youthpolicy.dto.SavedPolicyDto;
import com.youthjob.api.youthpolicy.repository.SavedPolicyRepository;
import com.youthjob.api.youthpolicy.repository.YouthPolicyRepository;
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
    private final YouthPolicyRepository policyRepository;
    private final UserRepository userRepository;

    public List<SavedPolicyDto> listSaved() {
        User me = currentUser();
        return savedPolicyRepository.findAllByUserOrderByCreatedAtDesc(me)
                .stream().map(SavedPolicyDto::from).toList();
    }

    @Transactional
    public SavedPolicyDto save(SavePolicyRequest req) {
        User me = currentUser();

        var existed = savedPolicyRepository.findByUserAndPlcyNo(me, req.plcyNo());
        if (existed.isPresent()) return SavedPolicyDto.from(existed.get());

        YouthPolicy policy = policyRepository.findByPlcyNo(req.plcyNo())
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다: " + req.plcyNo()));

        SavedPolicy saved = new SavedPolicy(
                me,
                policy.getPlcyNo(),
                policy.getPlcyNm(),
                policy.getPlcyKywdNm(),
                policy.getLclsfNm(),
                policy.getMclsfNm(),
                policy.getAplyYmd(),
                policy.getAplyUrlAddr(),
                policy.getSprvsnInstCdNm(),
                policy.getOperInstCdNm()
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

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + auth.getName()));
    }
}
