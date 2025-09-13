package com.youthjob.api.empprogram.repository;

import com.youthjob.api.empprogram.domain.SavedEmpProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SavedEmpProgramRepository extends JpaRepository<SavedEmpProgram, Long> {
    boolean existsByMemberIdAndExtKey(Long memberId, String extKey);
    Optional<SavedEmpProgram> findByMemberIdAndExtKey(Long memberId, String extKey);
    List<SavedEmpProgram> findByMemberIdOrderByIdDesc(Long memberId);

    Page<SavedEmpProgram> findByMemberId(Long memberId, Pageable pageable);
    long countByMemberId(Long memberId);
}
