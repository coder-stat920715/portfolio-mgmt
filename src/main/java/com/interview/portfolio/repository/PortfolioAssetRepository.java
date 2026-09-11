package com.interview.portfolio.repository;

import com.interview.portfolio.entity.PortfolioAsset;
import com.interview.portfolio.entity.PortfolioAssetId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository keyed by the @EmbeddedId composite key type (PortfolioAssetId), not a
 * simple Long - Spring Data JPA supports composite keys as the ID type parameter
 * transparently as long as equals()/hashCode() are correctly implemented.
 */
public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, PortfolioAssetId> {
}
