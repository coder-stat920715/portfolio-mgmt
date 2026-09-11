package com.interview.portfolio.repository;

import com.interview.portfolio.dto.AccountBalanceProjection;
import com.interview.portfolio.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Interview concept - Pessimistic locking via Spring Data JPA.
     * @Lock(LockModeType.PESSIMISTIC_WRITE) translates to a "SELECT ... FOR UPDATE" on
     * supported databases. The row-level lock is held until the enclosing transaction
     * commits or rolls back, blocking any other transaction attempting to
     * read-for-update or write the same row. This is used in TradeExecutionService to
     * serialize concurrent trades against the same account and prevent lost updates
     * WITHOUT relying on optimistic retry loops.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    /**
     * Native SQL query + interface projection: demonstrates fetching a computed,
     * cross-table result set (join + aggregate) as a lightweight DTO instead of
     * hydrating full Account/User/Portfolio entities.
     */
    @Query(value = """
            SELECT a.account_number   AS accountNumber,
                   a.balance          AS balance,
                   u.username         AS username,
                   COUNT(p.id)        AS portfolioCount
            FROM accounts a
            JOIN users u ON u.id = a.user_id
            LEFT JOIN portfolios p ON p.account_id = a.id
            WHERE a.account_number = :accountNumber
            GROUP BY a.account_number, a.balance, u.username
            """, nativeQuery = true)
    Optional<AccountBalanceProjection> findBalanceProjectionByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * JOIN FETCH solves the classic N+1: without it, calling account.getPortfolios()
     * on each Account in the returned list triggers one extra SELECT per account.
     * The DISTINCT keyword is required because the SQL-level JOIN duplicates the parent
     * row once per child row; Hibernate applies "distinct" at the Java object level
     * for JPQL queries (post Hibernate 5.2+ this no longer requires DISTINCT in SQL).
     */
    @Query("select distinct a from Account a join fetch a.portfolios where a.user.id = :userId")
    List<Account> findAllWithPortfoliosByUserId(@Param("userId") Long userId);
}
