package com.youthjob.api.youthpolicy.repository;

import com.youthjob.api.auth.domain.User;
import com.youthjob.api.youthpolicy.domain.SavedPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SavedPolicyRepository extends JpaRepository<SavedPolicy, Long> {
    List<SavedPolicy> findAllByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndPlcyNo(User user, String plcyNo);
    Optional<SavedPolicy> findByIdAndUser(Long id, User user);
    Optional<SavedPolicy> findByUserAndPlcyNo(User user, String plcyNo);

    // === 추가: 마이페이지용 ===
    Page<SavedPolicy> findByUser(User user, Pageable pageable);
    long countByUser(User user);
}
