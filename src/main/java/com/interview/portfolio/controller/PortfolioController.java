package com.interview.portfolio.controller;

import com.interview.portfolio.dto.AssetSearchCriteria;
import com.interview.portfolio.dto.PortfolioSummaryDTO;
import com.interview.portfolio.entity.Asset;
import com.interview.portfolio.entity.Portfolio;
import com.interview.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/account/{accountId}/summaries")
    public List<PortfolioSummaryDTO> getSummariesForAccount(@PathVariable Long accountId) {
        return portfolioService.getPortfolioSummariesForAccount(accountId);
    }

    @GetMapping("/{portfolioId}/summary")
    public PortfolioSummaryDTO getSummary(@PathVariable Long portfolioId) {
        return portfolioService.getPortfolioSummaryById(portfolioId);
    }

    @GetMapping("/account/{accountId}")
    public Page<Portfolio> getPortfoliosPaged(@PathVariable Long accountId,
                                               @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return portfolioService.getPortfoliosForAccount(accountId, pageable);
    }

    @GetMapping("/assets/search")
    public Page<Asset> searchAssets(@RequestParam(required = false) String symbolContains,
                                     @RequestParam(required = false) String assetType,
                                     @RequestParam(required = false) BigDecimal minPrice,
                                     @RequestParam(required = false) BigDecimal maxPrice,
                                     @RequestParam(required = false) String sector,
                                     @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
        AssetSearchCriteria criteria = AssetSearchCriteria.builder()
                .symbolContains(symbolContains)
                .assetType(assetType)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sector(sector)
                .build();
        return portfolioService.searchAssets(criteria, pageable);
    }
}
