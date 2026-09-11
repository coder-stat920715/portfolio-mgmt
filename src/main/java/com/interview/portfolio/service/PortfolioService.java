package com.interview.portfolio.service;

import com.interview.portfolio.dto.AssetSearchCriteria;
import com.interview.portfolio.dto.PortfolioSummaryDTO;
import com.interview.portfolio.entity.Asset;
import com.interview.portfolio.entity.Portfolio;
import com.interview.portfolio.entity.PortfolioAsset;
import com.interview.portfolio.exception.ResourceNotFoundException;
import com.interview.portfolio.repository.AssetRepository;
import com.interview.portfolio.repository.AssetSpecifications;
import com.interview.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;

    /**
     * N+1 DEMONSTRATION (JOIN FETCH variant).
     * Without JOIN FETCH, iterating `portfolios` and calling p.getPortfolioAssets() on
     * each would issue: 1 query for the portfolio list + N queries (one per portfolio)
     * to lazily initialize each portfolio's collection = N+1 total round trips.
     * findAllWithAssetsByAccountId uses "join fetch" so this whole method executes
     * exactly ONE SQL query regardless of how many portfolios/assets exist.
     */
    @Transactional(readOnly = true)
    public List<PortfolioSummaryDTO> getPortfolioSummariesForAccount(Long accountId) {
        List<Portfolio> portfolios = portfolioRepository.findAllWithAssetsByAccountId(accountId);
        return portfolios.stream().map(this::toSummaryDTO).toList();
    }

    /**
     * N+1 DEMONSTRATION (@EntityGraph variant) - same outcome as above, different
     * mechanism: Spring Data reads the @EntityGraph metadata on
     * PortfolioRepository#findWithAssetsById and dynamically constructs the fetch plan,
     * again collapsing what would otherwise be 1 + N + N (nested) queries into one.
     */
    @Transactional(readOnly = true)
    public PortfolioSummaryDTO getPortfolioSummaryById(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findWithAssetsById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        return toSummaryDTO(portfolio);
    }

    @Transactional(readOnly = true)
    public Page<Portfolio> getPortfoliosForAccount(Long accountId, Pageable pageable) {
        return portfolioRepository.findAllByAccountId(accountId, pageable);
    }

    /**
     * Dynamic, type-safe search built from an optional-fields criteria object using
     * Spring Data Specifications (see AssetSpecifications), combined with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Asset> searchAssets(AssetSearchCriteria criteria, Pageable pageable) {
        return assetRepository.findAll(AssetSpecifications.fromCriteria(criteria), pageable);
    }

    private PortfolioSummaryDTO toSummaryDTO(Portfolio portfolio) {
        List<PortfolioSummaryDTO.AssetHoldingDTO> holdings = portfolio.getPortfolioAssets().stream()
                .map(this::toHoldingDTO)
                .toList();

        BigDecimal totalMarketValue = holdings.stream()
                .map(h -> h.getCurrentPrice().multiply(h.getUnits()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioSummaryDTO.builder()
                .portfolioId(portfolio.getId())
                .name(portfolio.getName())
                .accountNumber(portfolio.getAccount() != null ? portfolio.getAccount().getAccountNumber() : null)
                .totalMarketValue(totalMarketValue)
                .holdings(holdings)
                .build();
    }

    private PortfolioSummaryDTO.AssetHoldingDTO toHoldingDTO(PortfolioAsset link) {
        return PortfolioSummaryDTO.AssetHoldingDTO.builder()
                .symbol(link.getAsset().getSymbol())
                .assetName(link.getAsset().getName())
                .units(link.getUnits())
                .allocationPercentage(link.getAllocationPercentage())
                .currentPrice(link.getAsset().getCurrentPrice())
                .build();
    }
}
