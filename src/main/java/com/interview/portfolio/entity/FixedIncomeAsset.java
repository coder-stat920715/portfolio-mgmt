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
import java.time.LocalDate;

@Entity
@Table(name = "fixed_income_assets")
@PrimaryKeyJoinColumn(name = "asset_id")
@DiscriminatorValue("FIXED_INCOME")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FixedIncomeAsset extends Asset {

    @Column(precision = 5, scale = 3)
    private BigDecimal couponRate;

    @Column
    private LocalDate maturityDate;

    @Column(length = 10)
    private String creditRating;
}
