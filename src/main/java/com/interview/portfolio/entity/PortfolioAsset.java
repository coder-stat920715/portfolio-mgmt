package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Materialized many-to-many "link" entity between Portfolio and Asset.
 *
 * Interview concept - @MapsId:
 *   Rather than duplicating the FK columns AND the @EmbeddedId fields, @MapsId tells
 *   Hibernate "the portfolioId part of my composite key IS the FK to Portfolio, don't make
 *   me set it twice." Hibernate derives portfolio_id / asset_id directly from the
 *   associated entities' identifiers when the entity is persisted.
 */
@Entity
@Table(name = "portfolio_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"portfolio", "asset"})
@EqualsAndHashCode(of = "id")
public class PortfolioAsset {

    @EmbeddedId
    private PortfolioAssetId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("portfolioId")
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assetId")
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPercentage;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal units;
}
