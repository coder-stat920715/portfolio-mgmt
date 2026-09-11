package com.interview.portfolio.service;

import com.interview.portfolio.entity.Account;
import com.interview.portfolio.exception.ResourceNotFoundException;
import com.interview.portfolio.exception.TradeConflictException;
import com.interview.portfolio.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handles cash-balance mutations that are OPTIMISTICALLY locked via Account.version.
 *
 * Interview concept - retrying on OptimisticLockingFailureException:
 *   Because optimistic locking detects conflicts only at commit time (via the failed
 *   version-checked UPDATE), the idiomatic pattern is: catch the failure, reload the
 *   fresh entity state, and re-apply the business operation, up to a bounded number
 *   of attempts, before giving up. This trades a small amount of latency under
 *   contention for zero held database locks during the "thinking" portion of the
 *   transaction - appropriate here because balance top-ups/withdrawals are user-driven
 *   and relatively low-frequency compared to trade execution.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int MAX_RETRIES = 3;

    private final AccountRepository accountRepository;

    /**
     * Propagation.REQUIRED (the default, stated explicitly for interview visibility):
     * joins the caller's transaction if one is already active, otherwise starts a new one.
     * Isolation.READ_COMMITTED (also the common RDBMS default): prevents dirty reads of
     * uncommitted balance changes from other transactions, while still allowing
     * non-repeatable reads - acceptable here because @Version protects the actual write.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Account adjustBalance(Long accountId, BigDecimal delta) {
        int attempt = 0;
        while (true) {
            try {
                Account account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

                BigDecimal newBalance = account.getBalance().add(delta);
                if (newBalance.signum() < 0) {
                    throw new com.interview.portfolio.exception.InsufficientBalanceException(
                            "Insufficient balance for account " + account.getAccountNumber());
                }
                account.setBalance(newBalance);
                // save() here triggers the version-checked UPDATE on flush/commit
                return accountRepository.save(account);

            } catch (OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new TradeConflictException(
                            "Failed to adjust balance after " + MAX_RETRIES + " attempts due to concurrent updates", ex);
                }
                // loop again: re-fetch the now-current row and re-apply the delta
            }
        }
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }
}
