package com.interview.portfolio.repository;

import com.interview.portfolio.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Approach 1 to solving N+1: explicit JPQL "JOIN FETCH".
     * Eagerly initializes portfolioAssets (and, transitively via a nested fetch, the
     * Asset each link points to) in a SINGLE round-trip SQL query, overriding the
     * LAZY fetch type declared on the entity mapping for just this query.
     */
    @Query("""
            select distinct p from Portfolio p
            left join fetch p.portfolioAssets pa
            left join fetch pa.asset
            where p.account.id = :accountId
            """)
    List<Portfolio> findAllWithAssetsByAccountId(@Param("accountId") Long accountId);

    /**
     * Approach 2 to solving N+1: declarative @EntityGraph.
     * Functionally equivalent outcome to the JOIN FETCH query above, but expressed as
     * metadata rather than JPQL - Spring Data dynamically builds the fetch plan.
     * attributePaths supports dotted nested paths ("portfolioAssets.asset") to reach
     * into associations-of-associations, same as the nested "join fetch" above.
     */
    @EntityGraph(attributePaths = {"portfolioAssets", "portfolioAssets.asset"})
    Optional<Portfolio> findWithAssetsById(Long id);

    Page<Portfolio> findAllByAccountId(Long accountId, Pageable pageable);
}
