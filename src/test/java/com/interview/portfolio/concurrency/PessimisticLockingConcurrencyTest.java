package com.interview.portfolio.concurrency;

import com.interview.portfolio.entity.Account;
import com.interview.portfolio.entity.EquityAsset;
import com.interview.portfolio.entity.TradeOrder;
import com.interview.portfolio.entity.User;
import com.interview.portfolio.repository.AccountRepository;
import com.interview.portfolio.repository.AssetRepository;
import com.interview.portfolio.repository.UserRepository;
import com.interview.portfolio.service.TradeExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that PESSIMISTIC_WRITE row locking (findByIdForUpdate) serializes concurrent
 * SELL trades against the same account: unlike the optimistic-locking test (where all
 * threads race and some retry after failing), here every thread should succeed on its
 * FIRST attempt because contending threads simply BLOCK and wait for the lock rather
 * than fail and retry - the balance still ends up correct either way, but the mechanism
 * and cost profile (blocking vs. retrying) are fundamentally different, which is the
 * whole point of this comparison test.
 */
@SpringBootTest
@ActiveProfiles("test")
class PessimisticLockingConcurrencyTest {

    @Autowired
    private TradeExecutionService tradeExecutionService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AssetRepository assetRepository;

    private Long accountId;
    private Long assetId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("pess-lock-user-" + System.nanoTime())
                .email("pess" + System.nanoTime() + "@example.com")
                .password("x").build());
        Account account = accountRepository.save(Account.builder()
                .accountNumber("ACC-PESS-" + System.nanoTime())
                .balance(new BigDecimal("100000.00")) // large starting balance so BUY orders never reject
                .user(user)
                .build());
        accountId = account.getId();

        EquityAsset asset = assetRepository.save(EquityAsset.builder()
                .symbol("GOOG-" + System.nanoTime() % 100000)
                .name("Alphabet Inc")
                .currentPrice(new BigDecimal("100.00"))
                .sector("Tech")
                .build());
        assetId = asset.getId();
    }

    @Test
    void concurrentTrades_areSerializedByRowLockAndBalanceStaysConsistent() throws InterruptedException {
        int threadCount = 8;
        BigDecimal quantityPerOrder = BigDecimal.ONE; // buys 1 unit @ 100.00 each

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    tradeExecutionService.executeTrade(accountId, assetId, TradeOrder.OrderType.BUY, quantityPerOrder);
                } catch (Exception ex) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        Account finalAccount = accountRepository.findById(accountId).orElseThrow();
        BigDecimal expectedDebit = new BigDecimal("100.00").multiply(BigDecimal.valueOf(threadCount));

        assertThat(failures.get()).isZero();
        assertThat(finalAccount.getBalance())
                .as("No lost updates should occur - every BUY order's debit must be reflected exactly once")
                .isEqualByComparingTo(new BigDecimal("100000.00").subtract(expectedDebit));
    }
}
