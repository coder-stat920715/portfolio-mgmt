package com.interview.portfolio.repository;

import com.interview.portfolio.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Extends JpaSpecificationExecutor to enable dynamic, type-safe Criteria-API-backed
 * queries built at runtime from AssetSearchCriteria (see AssetSpecifications).
 */
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    Page<Asset> findAll(org.springframework.data.jpa.domain.Specification<Asset> spec, Pageable pageable);

    Optional<Asset> findBySymbol(String symbol);

    /**
     * QueryHints enabling the Hibernate query cache for this specific, frequently
     * repeated, read-only lookup - works together with the L2 entity cache: the query
     * cache stores the list of matching entity IDs, the L2 entity cache stores the
     * actual entity state, so a cache hit here can be satisfied without touching the DB.
     */
    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    @Query("select a from Asset a where a.currentPrice between :min and :max")
    java.util.List<Asset> findAllPricedBetweenCacheable(@Param("min") java.math.BigDecimal min,
                                                          @Param("max") java.math.BigDecimal max);
}
