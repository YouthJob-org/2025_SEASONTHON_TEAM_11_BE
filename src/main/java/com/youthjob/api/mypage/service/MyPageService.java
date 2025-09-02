package com.youthjob.api.mypage.service;

import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.api.hrd.dto.SavedCourseDto;
import com.youthjob.api.hrd.repository.SavedCourseRepository;
import com.youthjob.api.mypage.dto.*;
import com.youthjob.api.youthpolicy.dto.SavedPolicyDto;
import com.youthjob.api.youthpolicy.repository.SavedPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    // private final SavedCapabilityRepository savedCapabilityRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final SavedPolicyRepository savedPolicyRepository;

    /** 상단 프로필 + 카운트 */
    public MyPageSummaryDto summary() {
        User me = currentUser();

        // long capCnt  = savedCapabilityRepository.countByUser(me);
        long hrdCnt  = savedCourseRepository.countByUser(me);
        long polCnt  = savedPolicyRepository.countByUser(me);

        return MyPageSummaryDto.builder()
                .profile(toProfileDto(me))
                .counters(CountersDto.builder()
                        // .savedCapabilities(capCnt)
                        .savedCourses(hrdCnt)
                        .savedPolicies(polCnt)
                        .build())
                .build();
    }

    /** 내 정보 단독 조회 (수정 화면 초기값) */
    public ProfileDto profile() {
        return toProfileDto(currentUser());
    }

    /** 내 정보 수정: 이름만 부분 업데이트 */
    @Transactional
    public ProfileDto updateProfile(UpdateMyInfoRequest req) {
        User me = currentUser();
        me.updateName(req.name());
        return toProfileDto(me); // dirty checking
    }

//    /** 취업역량 강화 프로그램 (관심) 페이징 */
//    public PageResult<SavedCapabilityDto> savedCapabilities(int page, int size) {
//        User me = currentUser();
//        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100),
//                Sort.by(Sort.Direction.DESC, "createdAt"));
//        var p = savedCapabilityRepository.findByUser(me, pageable)
//                .map(SavedCapabilityDto::from);
//        return PageResult.<SavedCapabilityDto>builder()
//                .page(p.getNumber()).size(p.getSize())
//                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
//                .items(p.getContent())
//                .build();
//    }

    /** 내일배움카드(관심) 페이징 */
    public PageResult<SavedCourseDto> savedCourses(int page, int size) {
        User me = currentUser();
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = savedCourseRepository.findByUser(me, pageable)
                .map(SavedCourseDto::from);
        return PageResult.<SavedCourseDto>builder()
                .page(p.getNumber()).size(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .items(p.getContent())
                .build();
    }

    /** 청년정책(관심) 페이징 */
    public PageResult<SavedPolicyDto> savedPolicies(int page, int size) {
        User me = currentUser();
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = savedPolicyRepository.findByUser(me, pageable)
                .map(SavedPolicyDto::from);
        return PageResult.<SavedPolicyDto>builder()
                .page(p.getNumber()).size(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .items(p.getContent())
                .build();
    }

    // ===== 내부 유틸 =====
    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException("인증 필요");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + auth.getName()));
    }

    private ProfileDto toProfileDto(User me) {
        String displayName = (me.getName() != null && !me.getName().isBlank())
                ? me.getName()
                : deriveLocalPart(me.getEmail());

        return ProfileDto.builder()
                .displayName(displayName)
                .email(me.getEmail())
                .joinedAt(me.getCreatedAt())
                .name(me.getName())
                .build();
    }
    private String deriveLocalPart(String email) {
        if (email == null) return "사용자";
        int idx = email.indexOf('@');
        return idx > 0 ? email.substring(0, idx) : email;
    }
}
