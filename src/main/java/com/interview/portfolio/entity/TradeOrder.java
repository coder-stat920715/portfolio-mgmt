package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * TradeOrder is the high-write, high-contention entity in this domain - many concurrent
 * BUY/SELL requests may target the same Account/Asset simultaneously. This is the entity
 * used to demonstrate PESSIMISTIC_WRITE locking in TradeExecutionService, since here we
 * WANT competing transactions to queue up and wait rather than fail-and-retry (which is
 * what optimistic locking would do under heavy contention -> thrashing / high abort rate).
 */
@Entity
@Table(name = "trade_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"account", "asset"})
@EqualsAndHashCode(of = "id")
public class TradeOrder extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType orderType;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    public enum OrderType { BUY, SELL }

    public enum OrderStatus { PENDING, EXECUTED, REJECTED, CANCELLED }
}
