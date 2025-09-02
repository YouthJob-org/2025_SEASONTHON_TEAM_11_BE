package com.youthjob.api.hrd.repository;


import com.youthjob.api.auth.domain.User;
import com.youthjob.api.hrd.domain.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
    boolean existsByUserAndTrprIdAndTrprDegr(User user, String trprId, String trprDegr);
    List<SavedCourse> findAllByUserOrderByCreatedAtDesc(User user);
    Optional<SavedCourse> findByIdAndUser(Long id, User user);

    // === 추가: 마이페이지용 ===
    Page<SavedCourse> findByUser(User user, Pageable pageable);
    long countByUser(User user);
}
