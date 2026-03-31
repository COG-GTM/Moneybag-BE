package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByBatchId(String batchId);

    List<PayrollRecord> findByBatchIdAndStatus(String batchId, String status);

    Optional<PayrollRecord> findByBatchIdAndParticipantId(String batchId, Long participantId);
}
