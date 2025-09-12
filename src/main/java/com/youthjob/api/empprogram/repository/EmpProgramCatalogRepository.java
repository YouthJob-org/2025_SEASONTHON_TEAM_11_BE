package com.youthjob.api.empprogram.repository;

import com.youthjob.api.empprogram.domain.EmpProgramCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmpProgramCatalogRepository extends JpaRepository<EmpProgramCatalog, Long> {

    boolean existsByExtKey(String extKey);

    @Modifying
    @Query("delete from EmpProgramCatalog e where e.pgmEndt < ?1")
    int deleteAllEndedBefore(String yyyymmdd);
}
