package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.ParticipantElection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantElectionRepository extends JpaRepository<ParticipantElection, Long> {

    List<ParticipantElection> findByParticipantIdAndPlanYear(Long participantId, int planYear);

    Optional<ParticipantElection> findByParticipantIdAndPlanYearAndStatus(Long participantId, int planYear, String status);
}
