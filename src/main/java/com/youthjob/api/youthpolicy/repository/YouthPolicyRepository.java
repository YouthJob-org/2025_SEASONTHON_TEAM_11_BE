package com.youthjob.api.youthpolicy.repository;

import com.youthjob.api.youthpolicy.domain.YouthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface YouthPolicyRepository
        extends JpaRepository<YouthPolicy, Long>, JpaSpecificationExecutor<YouthPolicy> {

    Optional<YouthPolicy> findByPlcyNo(String plcyNo);

    List<YouthPolicy> findAllByPlcyNoIn(Collection<String> plcyNos);
}
