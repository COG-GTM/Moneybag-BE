package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.ContributionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContributionSummaryRepository extends JpaRepository<ContributionSummary, Long> {

    Optional<ContributionSummary> findByParticipantIdAndPlanYear(Long participantId, int planYear);
}
