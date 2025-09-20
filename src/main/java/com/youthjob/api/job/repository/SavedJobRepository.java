package com.youthjob.api.job.repository;

import com.youthjob.api.job.domain.SavedJob;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.*;

import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    boolean existsByUserIdAndJob_ExternalId(String userId, String externalId);

    void deleteByUserIdAndJob_ExternalId(String userId, String externalId);

    @EntityGraph(attributePaths = "job")
    Optional<SavedJob> findWithJobByUserIdAndJob_ExternalId(String userId, String externalId);

    @EntityGraph(attributePaths = "job")
    Page<SavedJob> findAllByUserId(String userId, Pageable pageable);
}
