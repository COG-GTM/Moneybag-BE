package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.PayrollBatchSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollBatchSummaryRepository extends JpaRepository<PayrollBatchSummary, Long> {
    Optional<PayrollBatchSummary> findByBatchId(String batchId);
}
