package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.RetirementPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetirementPlanRepository extends JpaRepository<RetirementPlan, Long> {
}
