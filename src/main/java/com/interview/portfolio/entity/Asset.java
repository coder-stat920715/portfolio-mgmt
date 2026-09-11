package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Interview concept - @Inheritance(strategy = InheritanceType.JOINED):
 *   - A base table "assets" holds shared columns (id, symbol, name, currentPrice, assetClass).
 *   - Each subclass (EquityAsset, FixedIncomeAsset) gets its OWN table containing only
 *     its specific columns, PLUS a PK column that is also a FK back to assets.id.
 *   - Loading a base-typed reference triggers a SQL JOIN across parent+child tables
 *     (hence "JOINED"). This is normalized (no wasted nullable columns, unlike
 *     SINGLE_TABLE) but costs a join on every read - a common trade-off discussion point
 *     versus SINGLE_TABLE (fast reads, denormalized/nullable columns) and
 *     TABLE_PER_CLASS (no joins, but breaks polymorphic queries/shared ID generation).
 *
 * Interview concept - @Cache (Hibernate L2 second-level cache):
 *   Asset data is read constantly (price lookups, portfolio valuation) but changes
 *   relatively infrequently compared to trade/transaction volume, making it an ideal
 *   L2 cache candidate. READ_WRITE concurrency strategy uses soft locks to keep the
 *   cache consistent with concurrent transactional writes, at some throughput cost
 *   versus READ_ONLY (which requires the entity is functionally immutable).
 */
@Entity
@Table(name = "assets")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "asset_class", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "portfolioAssets")
@EqualsAndHashCode(of = "id")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    /**
     * Inverse side of the Portfolio <-> Asset many-to-many join entity.
     * FetchType.LAZY - never eagerly walk every portfolio holding this asset.
     */
    @OneToMany(mappedBy = "asset", fetch = FetchType.LAZY)
    private List<PortfolioAsset> portfolioAssets = new ArrayList<>();
}
