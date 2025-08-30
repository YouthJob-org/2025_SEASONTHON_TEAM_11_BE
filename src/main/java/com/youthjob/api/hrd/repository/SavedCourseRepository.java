package com.youthjob.api.hrd.repository;


import com.youthjob.api.auth.domain.User;
import com.youthjob.api.hrd.domain.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    List<SavedCourse> findAllByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndTrprIdAndTrprDegr(User user, String trprId, String trprDegr);

    Optional<SavedCourse> findByIdAndUser(Long id, User user);
}
