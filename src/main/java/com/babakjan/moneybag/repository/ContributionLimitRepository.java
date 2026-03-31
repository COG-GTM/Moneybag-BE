package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.ContributionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContributionLimitRepository extends JpaRepository<ContributionLimit, Long> {

    Optional<ContributionLimit> findByTaxYear(Integer taxYear);
}
