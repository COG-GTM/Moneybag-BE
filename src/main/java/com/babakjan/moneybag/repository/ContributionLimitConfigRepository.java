package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.ContributionLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContributionLimitConfigRepository extends JpaRepository<ContributionLimitConfig, Long> {

    Optional<ContributionLimitConfig> findByPlanYear(int planYear);
}
