package com.interview.portfolio.concurrency;

import com.interview.portfolio.entity.Account;
import com.interview.portfolio.entity.User;
import com.interview.portfolio.repository.AccountRepository;
import com.interview.portfolio.repository.UserRepository;
import com.interview.portfolio.service.AccountService;
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
 * @SpringBootTest boots the FULL application context (unlike @DataJpaTest's JPA slice),
 * which is necessary here because we need real @Transactional proxy behavior across
 * separate threads/transactions to genuinely exercise Hibernate's optimistic lock
 * version-check - a single-threaded, single-transaction @DataJpaTest cannot reproduce
 * a real version conflict.
 */
@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingConcurrencyTest {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("opt-lock-user-" + System.nanoTime())
                .email("opt" + System.nanoTime() + "@example.com")
                .password("x").build());
        Account account = accountRepository.save(Account.builder()
                .accountNumber("ACC-OPT-" + System.nanoTime())
                .balance(new BigDecimal("1000.00"))
                .user(user)
                .build());
        accountId = account.getId();
    }

    @Test
    void concurrentBalanceAdjustments_allSucceedEventuallyAndBalanceIsCorrect() throws InterruptedException {
        int threadCount = 10;
        BigDecimal depositPerThread = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // AccountService.adjustBalance internally retries on
                    // OptimisticLockingFailureException, so despite 10 threads racing
                    // to update the SAME row's version column, every call here should
                    // eventually succeed rather than propagate a conflict exception.
                    accountService.adjustBalance(accountId, depositPerThread);
                } catch (Exception ex) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        Account finalAccount = accountRepository.findById(accountId).orElseThrow();

        assertThat(failures.get()).isZero();
        assertThat(finalAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00").add(depositPerThread.multiply(BigDecimal.valueOf(threadCount))));
    }
}
