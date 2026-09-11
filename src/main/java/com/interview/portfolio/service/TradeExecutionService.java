package com.interview.portfolio.service;

import com.interview.portfolio.dto.TradeOrderResponseDTO;
import com.interview.portfolio.entity.*;
import com.interview.portfolio.exception.InsufficientBalanceException;
import com.interview.portfolio.exception.ResourceNotFoundException;
import com.interview.portfolio.repository.AccountRepository;
import com.interview.portfolio.repository.AssetRepository;
import com.interview.portfolio.repository.TradeOrderRepository;
import com.interview.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Executes BUY/SELL TradeOrders against an Account's cash balance.
 *
 * Interview concept - Pessimistic Locking (LockModeType.PESSIMISTIC_WRITE) via
 * AccountRepository#findByIdForUpdate:
 *   Trade execution is a high-frequency, high-contention write path - many orders can
 *   arrive for the same account within milliseconds. Rather than let each transaction
 *   optimistically race to commit and force most of them to retry (as AccountService
 *   does for simple top-ups), we acquire a row-level "SELECT ... FOR UPDATE" lock on the
 *   Account row up front. Every other transaction attempting to lock (or plain-write) the
 *   SAME account row blocks until this transaction commits/rolls back, guaranteeing
 *   serialized, lost-update-free balance mutations for the trade-execution hot path.
 *
 * Interview concept - Isolation.SERIALIZABLE:
 *   Combined with the explicit row lock, SERIALIZABLE here is largely belt-and-braces
 *   for this single-row mutation, but is called out to prompt discussion of the
 *   READ_UNCOMMITTED -> READ_COMMITTED -> REPEATABLE_READ -> SERIALIZABLE ladder and
 *   the phenomena (dirty/non-repeatable/phantom reads) each isolation level prevents.
 */
@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final AccountRepository accountRepository;
    private final AssetRepository assetRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService; // separate bean -> REQUIRES_NEW proxy applies correctly

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public TradeOrderResponseDTO executeTrade(Long accountId, Long assetId, TradeOrder.OrderType orderType, BigDecimal quantity) {

        // Acquire PESSIMISTIC_WRITE lock -> blocks concurrent trades on this account until commit/rollback
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));

        BigDecimal orderValue = asset.getCurrentPrice().multiply(quantity);

        TradeOrder order = TradeOrder.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .account(account)
                .asset(asset)
                .orderType(orderType)
                .quantity(quantity)
                .price(asset.getCurrentPrice())
                .status(TradeOrder.OrderStatus.PENDING)
                .build();

        try {
            if (orderType == TradeOrder.OrderType.BUY) {
                if (account.getBalance().compareTo(orderValue) < 0) {
                    order.setStatus(TradeOrder.OrderStatus.REJECTED);
                    tradeOrderRepository.save(order);
                    // Audit persists REGARDLESS of this method's eventual outcome/rollback
                    auditLogService.record("TradeOrder", order.getId(), "REJECTED", "SYSTEM",
                            "Insufficient balance for BUY order " + order.getOrderNumber());
                    throw new InsufficientBalanceException(
                            "Insufficient balance for account " + account.getAccountNumber());
                }
                account.setBalance(account.getBalance().subtract(orderValue));
            } else { // SELL
                account.setBalance(account.getBalance().add(orderValue));
            }

            order.setStatus(TradeOrder.OrderStatus.EXECUTED);
            TradeOrder savedOrder = tradeOrderRepository.save(order);

            // account.save() is technically redundant under dirty checking (see note below)
            // but kept explicit here for clarity in an interview walkthrough.
            Account savedAccount = accountRepository.save(account);

            Transaction transaction = Transaction.builder()
                    .tradeOrder(savedOrder)
                    .account(savedAccount)
                    .transactionType(orderType == TradeOrder.OrderType.BUY
                            ? Transaction.TransactionType.DEBIT
                            : Transaction.TransactionType.CREDIT)
                    .amount(orderValue)
                    .balanceAfter(savedAccount.getBalance())
                    .build();
            transactionRepository.save(transaction);

            auditLogService.record("TradeOrder", savedOrder.getId(), "EXECUTED", "SYSTEM",
                    "Executed " + orderType + " of " + quantity + " " + asset.getSymbol());

            return TradeOrderResponseDTO.builder()
                    .orderId(savedOrder.getId())
                    .orderNumber(savedOrder.getOrderNumber())
                    .status(savedOrder.getStatus().name())
                    .assetSymbol(asset.getSymbol())
                    .quantity(quantity)
                    .executionPrice(asset.getCurrentPrice())
                    .accountBalanceAfter(savedAccount.getBalance())
                    .build();

        } catch (InsufficientBalanceException ex) {
            throw ex; // triggers rollback of THIS transaction only; the REQUIRES_NEW audit above already committed
        }
    }

    /*
     * Interview note on dirty checking: because `account` and `order` are managed
     * entities loaded within this same persistence context/transaction, Hibernate's
     * automatic dirty checking would flush the mutated `balance` field on commit even
     * WITHOUT an explicit accountRepository.save(account) call. The explicit save()
     * calls above are harmless (Spring Data's save() on an already-managed entity with
     * a non-null ID is a no-op merge) and are included purely for code readability.
     */
}
