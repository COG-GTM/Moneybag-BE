package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.PlanConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanConfigurationRepository extends JpaRepository<PlanConfiguration, Long> {
    Optional<PlanConfiguration> findByPlanId(String planId);
}
