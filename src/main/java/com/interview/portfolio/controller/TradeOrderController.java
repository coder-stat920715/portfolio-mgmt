package com.interview.portfolio.controller;

import com.interview.portfolio.dto.TradeOrderRequestDTO;
import com.interview.portfolio.dto.TradeOrderResponseDTO;
import com.interview.portfolio.entity.TradeOrder;
import com.interview.portfolio.service.TradeExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade-orders")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeExecutionService tradeExecutionService;

    /**
     * Executes a trade under PESSIMISTIC_WRITE locking. Firing many concurrent
     * requests at this endpoint for the SAME accountId is the way to observe row-lock
     * serialization behavior (requests queue and complete one-at-a-time rather than
     * racing/retrying as adjust-balance does).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TradeOrderResponseDTO executeTrade(@Valid @RequestBody TradeOrderRequestDTO request) {
        return tradeExecutionService.executeTrade(
                request.getAccountId(),
                request.getAssetId(),
                TradeOrder.OrderType.valueOf(request.getOrderType().toUpperCase()),
                request.getQuantity());
    }
}
