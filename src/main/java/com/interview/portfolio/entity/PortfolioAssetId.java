package com.interview.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Interview concept - @EmbeddedId composite primary key:
 *   Must implement Serializable and override equals()/hashCode() (Lombok's
 *   @EqualsAndHashCode does this for us) based on ALL key fields, because Hibernate uses
 *   these to manage the persistence context identity map and first-level cache lookups
 *   for this entity. An incorrect equals/hashCode on a composite key is a classic source
 *   of "duplicate row" or "entity not found" bugs.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PortfolioAssetId implements Serializable {

    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Column(name = "asset_id")
    private Long assetId;
}
