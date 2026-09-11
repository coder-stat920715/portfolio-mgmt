package com.interview.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderRequestDTO {

    @NotNull
    private Long accountId;

    @NotNull
    private Long assetId;

    @NotNull
    private String orderType; // BUY / SELL

    @NotNull
    @DecimalMin(value = "0.000001", message = "quantity must be positive")
    private BigDecimal quantity;
}
