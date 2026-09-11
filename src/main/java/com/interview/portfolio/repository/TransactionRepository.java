package com.interview.portfolio.repository;

import com.interview.portfolio.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findAllByAccountIdOrderByCreatedDateDesc(Long accountId, Pageable pageable);
}
