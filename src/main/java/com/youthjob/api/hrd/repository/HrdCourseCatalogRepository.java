package com.youthjob.api.hrd.repository;

import com.youthjob.api.hrd.domain.HrdCourseCatalog;
import com.youthjob.api.hrd.dto.HrdCourseRow;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update hrd_course_catalog
        set area1 = case
            when address like '서울%' then '11'
            when address like '부산%' then '26'
            when address like '대구%' then '27'
            when address like '인천%' then '28'
            when address like '광주%' then '29'
            when address like '대전%' then '30'
            when address like '울산%' then '31'
            when address like '세종%' then '36'
            when address like '경기%' then '41'
            when address like '강원%' or address like '강원특별자치도%' then '51'
            when address like '충북%' then '43'
            when address like '충남%' then '44'
            when address like '전북%' or address like '전북특별자치도%' then '45'
            when address like '전남%' then '46'
            when address like '경북%' then '47'
            when address like '경남%' then '48'
            when address like '제주%' or address like '제주시%' or address like '서귀포시%' then '50'
            else area1
        end
        where (area1 is null or area1 = '')
          and address is not null
        """, nativeQuery = true)
    int bulkBackfillArea1();


    // (1) area1이 있을 때: area1 동등 → 날짜 범위 순서 (인덱스와 일치)
    @Query("""
    select c from HrdCourseCatalog c
    where c.area1 = :area1
      and c.traStartDate <= :e and c.traEndDate >= :s
      and (:ncs is null or c.ncsCd like concat(:ncs, '%'))
    """)
    Slice<HrdCourseRow> findSliceByArea1(
            @Param("s") LocalDate s,
            @Param("e") LocalDate e,
            @Param("area1") String area1,
            @Param("ncs") String ncs,
            Pageable pageable);

    // (2) area1이 없을 때: 시작일 우선 + (선택) ncs prefix
    @Query("""
            select c from HrdCourseCatalog c
            where c.traStartDate <= :e and c.traEndDate >= :s
              and (:ncs is null or c.ncsCd like concat(:ncs, '%'))
            """)
    Slice<HrdCourseRow> findSliceNoArea1(
            @Param("s") LocalDate s,
            @Param("e") LocalDate e,
            @Param("ncs") String ncs,
            Pageable pageable);
}
