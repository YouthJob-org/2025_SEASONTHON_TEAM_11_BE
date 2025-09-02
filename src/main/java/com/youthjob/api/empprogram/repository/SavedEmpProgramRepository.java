package com.youthjob.api.empprogram.repository;

import com.youthjob.api.empprogram.domain.SavedEmpProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedEmpProgramRepository extends JpaRepository<SavedEmpProgram, Long> {
    boolean existsByMemberIdAndExtKey(Long memberId, String extKey);
    Optional<SavedEmpProgram> findByMemberIdAndExtKey(Long memberId, String extKey);
    List<SavedEmpProgram> findByMemberIdOrderByIdDesc(Long memberId);
}
