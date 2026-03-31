package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    List<Contribution> findByParticipantId(Long participantId);
}
