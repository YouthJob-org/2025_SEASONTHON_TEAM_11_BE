package com.youthjob.api.hrd.repository;

import com.youthjob.api.hrd.domain.HrdCourseCatalog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;


public interface HrdCourseCatalogRepository extends JpaRepository<HrdCourseCatalog, Long>, JpaSpecificationExecutor<HrdCourseCatalog> {

    @Modifying
    @Query("delete from HrdCourseCatalog c where c.traEndDate < :today")
    int deleteAllEndedBefore(@Param("today") LocalDate today);

    Optional<HrdCourseCatalog> findByTrprIdAndTrprDegr(String trprId, String trprDegr);
}
