package com.interview.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Class-based (as opposed to interface-based) DTO projection, populated manually in the
 * service layer after a JOIN FETCH / EntityGraph query. Useful when the response shape
 * needs nested collections or computed fields that a flat interface projection can't express.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSummaryDTO {
    private Long portfolioId;
    private String name;
    private String accountNumber;
    private BigDecimal totalMarketValue;
    private List<AssetHoldingDTO> holdings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssetHoldingDTO {
        private String symbol;
        private String assetName;
        private BigDecimal units;
        private BigDecimal allocationPercentage;
        private BigDecimal currentPrice;
    }
}
