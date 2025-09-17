package com.youthjob.api.hrd.repository;

import com.youthjob.api.hrd.domain.HrdCourseFull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrdCourseFullRepository extends JpaRepository<HrdCourseFull, Long> {
    Optional<HrdCourseFull> findByTrprIdAndTrprDegrAndTorgId(String trprId, String trprDegr, String torgId);
}
