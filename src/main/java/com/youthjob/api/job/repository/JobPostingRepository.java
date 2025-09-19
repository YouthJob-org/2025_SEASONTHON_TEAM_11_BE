
package com.youthjob.api.job.repository;

import com.youthjob.api.job.domain.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    Optional<JobPosting> findByExternalId(String externalId);
}
