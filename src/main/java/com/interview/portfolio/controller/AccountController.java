package com.interview.portfolio.controller;

import com.interview.portfolio.entity.Account;
import com.interview.portfolio.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    /**
     * Demonstrates the optimistic-lock-with-retry path. Concurrently hitting this
     * endpoint for the same account is the easiest way to reproduce/observe
     * ObjectOptimisticLockingFailureException retries in server logs.
     */
    @PostMapping("/{id}/adjust-balance")
    public Account adjustBalance(@PathVariable Long id, @RequestParam BigDecimal delta) {
        return accountService.adjustBalance(id, delta);
    }
}
