package com.youthjob.api.job.repository;

import com.youthjob.api.job.domain.JobPosting;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface JobPostingRepository
        extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {

    Optional<JobPosting> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
}