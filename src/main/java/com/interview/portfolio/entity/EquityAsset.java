package com.interview.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;

/**
 * Child table "equity_assets" (PK = FK to assets.id) under JOINED inheritance.
 * @PrimaryKeyJoinColumn is implicit/defaulted here (id -> id) but many teams declare it
 * explicitly for clarity in code review / interviews.
 */
@Entity
@Table(name = "equity_assets")
@PrimaryKeyJoinColumn(name = "asset_id")
@DiscriminatorValue("EQUITY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EquityAsset extends Asset {

    @Column(length = 60)
    private String sector;

    @Column(precision = 5, scale = 2)
    private BigDecimal dividendYield;

    @Column(length = 10)
    private String exchange;
}
