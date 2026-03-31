package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.TaxReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxReportEntryRepository extends JpaRepository<TaxReportEntry, Long> {

    List<TaxReportEntry> findByParticipantIdAndPlanYear(Long participantId, int planYear);
}
