package com.babakjan.moneybag.repository;

import com.babakjan.moneybag.entity.Account;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Get total balance of accounts (which are included in statistic) filtered by user id. If user id is null,
     * balance of all accounts.
     * @param userId user id.
     * @return total balance
     */
    @Query("select coalesce(sum(a.balance), 0) " +
            "from Account a " +
            "where (a.user.id = :userId or :userId is null) " +
            "and a.includeInStatistic"
    )
    Double getTotalBalance(@Param("userId") Long userId);

    /**
     * Get currencies of accounts ordered by id, without loading the accounts (or their users).
     * @param pageable page to return, e.g. PageRequest.of(0, 1) for the first account only
     * @return list of currencies
     */
    @Query("select a.currency from Account a order by a.id")
    List<String> findCurrenciesOrderById(Pageable pageable);
}
