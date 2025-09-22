package com.youthjob.api.job.service;

import com.youthjob.api.job.domain.JobPosting;
import com.youthjob.api.job.domain.SavedJob;
import com.youthjob.api.job.dto.JobPostingSaveRequest;
import com.youthjob.api.job.repository.JobPostingRepository;
import com.youthjob.api.job.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository repo;
    private final SavedJobRepository savedRepo;

    /** 목록 검색(동적 필터 + 페이징) */
    @Transactional(readOnly = true)
    public Page<JobPosting> search(
            String q,
            String region,
            LocalDate regFrom,
            LocalDate regTo,
            LocalDate ddlFrom,
            LocalDate ddlTo,
            Pageable pageable
    ) {
        Specification<JobPosting> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // 키워드: 제목/회사/지역/고용형태/급여 전방위 like
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("company"), like),
                        cb.like(root.get("region"), like),
                        cb.like(root.get("employmentType"), like),
                        cb.like(root.get("salary"), like)
                ));
            }

            if (region != null && !region.isBlank()) {
                ps.add(cb.like(root.get("region"), "%" + region.trim() + "%"));
            }

            if (regFrom != null) ps.add(cb.greaterThanOrEqualTo(root.get("regDate"), regFrom));
            if (regTo   != null) ps.add(cb.lessThanOrEqualTo(root.get("regDate"), regTo));

            if (ddlFrom != null) ps.add(cb.greaterThanOrEqualTo(root.get("deadline"), ddlFrom));
            if (ddlTo   != null) ps.add(cb.lessThanOrEqualTo(root.get("deadline"), ddlTo));

            return cb.and(ps.toArray(new Predicate[0]));
        };

        // 기본 정렬: 등록일 DESC, 마감일 ASC 보조
        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Order.desc("regDate"), Sort.Order.asc("deadline"))
                : pageable.getSort();

        Pageable fixed = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return repo.findAll(spec, fixed);
    }

    /** ID로 단건 조회 */
    @Transactional(readOnly = true)
    public JobPosting getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    /** externalId로 단건 조회 */
    @Transactional(readOnly = true)
    public JobPosting getByExternalId(String externalId) {
        return repo.findByExternalId(externalId).orElse(null);
    }

    /** 저장/업서트(externalId 기준) */
    @Transactional
    public JobPosting upsert(JobPostingSaveRequest r) {
        if (r.getExternalId() == null || r.getExternalId().isBlank()) {
            throw new IllegalArgumentException("externalId는 필수입니다.");
        }

        JobPosting entity = repo.findByExternalId(r.getExternalId())
                .orElseGet(() -> JobPosting.builder()
                        .externalId(r.getExternalId())
                        .build());

        entity.setTitle(n(r.getTitle()));
        entity.setCompany(n(r.getCompany()));
        entity.setRegion(n(r.getRegion()));
        entity.setDetailUrl(n(r.getDetailUrl()));
        entity.setRegDate(r.getRegDate());
        entity.setDeadline(r.getDeadline());
        entity.setEmploymentType(n(r.getEmploymentType()));
        entity.setSalary(n(r.getSalary()));

        return repo.save(entity);
    }

    private String n(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    /** 유저가 공고 저장 (externalId 기준) — 이미 있으면 false, 신규 저장이면 true 반환 */
    @Transactional
    public boolean saveForUser(String userId, String externalId) {
        JobPosting job = repo.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 externalId: " + externalId));

        if (savedRepo.existsByUserIdAndJob_ExternalId(userId, externalId)) {
            return false; // 이미 저장됨
        }
        savedRepo.save(SavedJob.builder()
                .userId(userId)
                .job(job)
                .build());
        return true;
    }

    /** 유저가 저장한 공고 해제 — 존재하면 true(삭제됨), 없으면 false */
    @Transactional
    public boolean removeSaved(String userId, String externalId) {
        if (!savedRepo.existsByUserIdAndJob_ExternalId(userId, externalId)) {
            return false;
        }
        savedRepo.deleteByUserIdAndJob_ExternalId(userId, externalId);
        return true;
    }

    /** 저장 토글 — 저장됐으면 해제하고 false, 없었으면 저장하고 true 반환 */
    @Transactional
    public boolean toggleSave(String userId, String externalId) {
        if (savedRepo.existsByUserIdAndJob_ExternalId(userId, externalId)) {
            savedRepo.deleteByUserIdAndJob_ExternalId(userId, externalId);
            return false; // 해제됨
        } else {
            JobPosting job = repo.findByExternalId(externalId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 externalId: " + externalId));
            savedRepo.save(SavedJob.builder().userId(userId).job(job).build());
            return true; // 저장됨
        }
    }

    /** 유저가 이 공고를 저장했는지 여부 */
    @Transactional(readOnly = true)
    public boolean isSaved(String userId, String externalId) {
        return savedRepo.existsByUserIdAndJob_ExternalId(userId, externalId);
    }

    /** 유저의 저장 목록 (JobPosting 리스트로 반환) */
    @Transactional(readOnly = true)
    public Page<JobPosting> listSavedJobs(String userId, Pageable pageable) {
        return savedRepo.findAllByUserId(userId, pageable)
                .map(SavedJob::getJob); // N+1 방지는 SavedJobRepository에서 @EntityGraph("job")로 처리 권장
    }
}
