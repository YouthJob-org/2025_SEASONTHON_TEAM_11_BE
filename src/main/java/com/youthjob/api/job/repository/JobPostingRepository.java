package com.youthjob.api.job.repository;

import com.youthjob.api.job.domain.JobPosting;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface JobPostingRepository
        extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {

    Optional<JobPosting> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);

    @Modifying
    @Transactional
    @Query("delete from JobPosting j where j.deadline < :today")
    int deleteAllDeadlineBefore(@Param("today") LocalDate today);
}