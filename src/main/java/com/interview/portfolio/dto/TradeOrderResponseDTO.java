package com.interview.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeOrderResponseDTO {
    private Long orderId;
    private String orderNumber;
    private String status;
    private String assetSymbol;
    private BigDecimal quantity;
    private BigDecimal executionPrice;
    private BigDecimal accountBalanceAfter;
}
