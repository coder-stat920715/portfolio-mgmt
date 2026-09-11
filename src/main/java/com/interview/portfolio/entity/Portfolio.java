package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio groups a set of Asset holdings for an Account.
 *
 * Interview concept - @ManyToMany via explicit join entity instead of a plain @ManyToMany:
 *   A "raw" @ManyToMany would only let you map the FK pair (portfolio_id, asset_id) with no
 *   room for extra columns. Real-world many-to-many relationships almost always carry
 *   metadata (here: allocationPercentage, units held). The standard, production-correct
 *   pattern is to model the join table as its OWN entity (PortfolioAsset) with a composite
 *   primary key via @EmbeddedId, and expose it as two @OneToMany relationships
 *   (Portfolio -> PortfolioAsset, Asset -> PortfolioAsset). See PortfolioAsset/PortfolioAssetId.
 */
@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"account", "portfolioAssets"})
@EqualsAndHashCode(of = "id")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) // portfolios change moderately often; READ_WRITE balances safety/perf
public class Portfolio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortfolioAsset> portfolioAssets = new ArrayList<>();

    public void addAsset(Asset asset, java.math.BigDecimal allocationPercentage, java.math.BigDecimal units) {
        PortfolioAsset link = new PortfolioAsset();
        link.setId(new PortfolioAssetId(this.id, asset.getId()));
        link.setPortfolio(this);
        link.setAsset(asset);
        link.setAllocationPercentage(allocationPercentage);
        link.setUnits(units);
        this.portfolioAssets.add(link);
        asset.getPortfolioAssets().add(link);
    }

    public void removeAsset(PortfolioAsset link) {
        portfolioAssets.remove(link);
        link.getAsset().getPortfolioAssets().remove(link);
        link.setPortfolio(null);
        link.setAsset(null);
    }
}
