package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.ParticipantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantProfileRepository extends JpaRepository<ParticipantProfile, Long> {

    Optional<ParticipantProfile> findByUserId(Long userId);
}
