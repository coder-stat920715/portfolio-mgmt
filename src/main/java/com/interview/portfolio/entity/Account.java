package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Account holds the cash balance used to fund trades.
 *
 * Interview concept - Optimistic Locking (@Version):
 *   Hibernate adds a hidden WHERE id = ? AND version = ? clause to every UPDATE and
 *   increments the version column atomically. If a concurrent transaction already changed
 *   the row (and bumped the version), the affected-row count comes back as 0 and Hibernate
 *   throws OptimisticLockException, which Spring translates into
 *   ObjectOptimisticLockingFailureException. This is cheap (no DB locks held) and ideal for
 *   the common case where balance updates rarely collide (e.g. user-initiated top-ups).
 *
 * Contrast this with Pessimistic Locking used in TradeExecutionService for the *trade
 * execution* path, where contention is expected/high-frequency (many concurrent BUY/SELL
 * orders hitting the same account) and we'd rather block a thread than retry failed writes.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "portfolios"})
@EqualsAndHashCode(of = "id")
public class Account extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /**
     * Optimistic lock token. Hibernate manages this column entirely - never set it manually.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Portfolio> portfolios = new ArrayList<>();

    public void addPortfolio(Portfolio portfolio) {
        portfolios.add(portfolio);
        portfolio.setAccount(this);
    }

    public void removePortfolio(Portfolio portfolio) {
        portfolios.remove(portfolio);
        portfolio.setAccount(null);
    }
}
