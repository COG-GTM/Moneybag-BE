package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.PlanConfigurationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanConfigurationAuditLogRepository extends JpaRepository<PlanConfigurationAuditLog, Long> {
    List<PlanConfigurationAuditLog> findByPlanIdOrderByTimestampDesc(String planId);
}
