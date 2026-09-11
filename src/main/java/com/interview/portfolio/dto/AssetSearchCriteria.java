package com.interview.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Filter carrier object for the dynamic Specification-based search
 * (see AssetSpecifications / AssetRepository#findAll(Specification, Pageable)).
 * Every field is optional/nullable - a null field simply means "don't filter on this".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSearchCriteria {
    private String symbolContains;
    private String assetType; // EQUITY / FIXED_INCOME
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sector; // only applies to EquityAsset
}
