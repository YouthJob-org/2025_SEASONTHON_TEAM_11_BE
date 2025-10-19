package com.youthjob.api.hrd.repository;


import com.youthjob.api.auth.domain.User;
import com.youthjob.api.hrd.domain.SavedCourse;
import com.youthjob.api.hrd.dto.SavedCourseView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
    boolean existsByUserAndTrprIdAndTrprDegr(User user, String trprId, String trprDegr);
    List<SavedCourse> findAllByUserOrderByCreatedAtDesc(User user);
    Optional<SavedCourse> findByIdAndUser(Long id, User user);


    Page<SavedCourse> findByUser(User user, Pageable pageable);
    long countByUser(User user);

    Optional<SavedCourse> findTopByUserAndTrprIdAndTrprDegrOrderByCreatedAtDesc(User u, String trprId, String trprDegr);

    @Query("""
    select new com.youthjob.api.hrd.dto.SavedCourseView(
        s.id,
        s.trprId, s.trprDegr, coalesce(c.torgId, s.torgId),
        c.title, c.subTitle,
        c.address, c.telNo,
        c.traStartDate, c.traEndDate,
        c.trainTarget, c.trainTargetCd,
        c.ncsCd,
        c.courseMan, c.realMan, c.yardMan,
        c.titleLink, c.subTitleLink,
        s.createdAt
    )
    from SavedCourse s
    left join HrdCourseCatalog c
        on c.trprId = s.trprId and c.trprDegr = s.trprDegr
    where s.user = :user
    order by s.createdAt desc
    """)
    List<SavedCourseView> findAllViewsByUser(@Param("user") User user);

    @Query("""
      select new com.youthjob.api.hrd.dto.SavedCourseView(
        sc.id, sc.trprId, sc.trprDegr,
        coalesce(hcc.torgId, sc.torgId),
        hcc.title, hcc.subTitle, hcc.address, hcc.telNo,
        hcc.traStartDate, hcc.traEndDate,
        hcc.trainTarget, hcc.trainTargetCd, hcc.ncsCd,
        hcc.courseMan, hcc.realMan, hcc.yardMan,
        hcc.titleLink, hcc.subTitleLink,
        sc.createdAt
      )
      from SavedCourse sc
      left join HrdCourseCatalog hcc
        on hcc.trprId = sc.trprId and hcc.trprDegr = sc.trprDegr
      where sc.user = :user
      order by sc.createdAt desc
    """)
    Page<SavedCourseView> findAllViewsByUser(@Param("user") User user, Pageable pageable);
}
