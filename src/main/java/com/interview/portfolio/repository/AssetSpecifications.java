package com.interview.portfolio.repository;

import com.interview.portfolio.dto.AssetSearchCriteria;
import com.interview.portfolio.entity.Asset;
import com.interview.portfolio.entity.EquityAsset;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Interview concept - Spring Data JPA Specifications.
 * Specification<T> is a thin, composable wrapper around the JPA Criteria API's
 * (Root, CriteriaQuery, CriteriaBuilder) -> Predicate function signature. Each static
 * factory method below returns a lambda implementing Specification<Asset>; they're then
 * composed with .and()/.or() at the call site to build a single dynamic, type-safe query
 * whose WHERE clause depends entirely on which criteria fields are non-null at runtime -
 * avoiding both (a) string-concatenated JPQL and (b) an explosion of hand-written
 * derived-query-method permutations.
 */
public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<Asset> fromCriteria(AssetSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getSymbolContains() != null && !criteria.getSymbolContains().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("symbol")),
                        "%" + criteria.getSymbolContains().toUpperCase() + "%"));
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), criteria.getMaxPrice()));
            }

            if (criteria.getAssetType() != null && !criteria.getAssetType().isBlank()) {
                // Interview concept: type() lets you filter polymorphically on the JOINED
                // inheritance discriminator without a raw SQL discriminator column check.
                predicates.add(cb.equal(root.type(), lookupAssetClass(criteria.getAssetType())));
            }

            if (criteria.getSector() != null && !criteria.getSector().isBlank()) {
                // treat() performs a downcast in the Criteria API so we can reference a
                // subclass-only attribute (sector) that doesn't exist on the base Asset type.
                predicates.add(cb.equal(cb.treat(root, EquityAsset.class).get("sector"), criteria.getSector()));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Class<? extends Asset> lookupAssetClass(String assetType) {
        return switch (assetType.toUpperCase()) {
            case "EQUITY" -> EquityAsset.class;
            case "FIXED_INCOME" -> com.interview.portfolio.entity.FixedIncomeAsset.class;
            default -> throw new IllegalArgumentException("Unknown asset type: " + assetType);
        };
    }
}
