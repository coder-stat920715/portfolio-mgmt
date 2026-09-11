package com.interview.portfolio.repository;

import com.interview.portfolio.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * TradeOrderRepositoryCustom is implemented manually in TradeOrderRepositoryImpl using a
 * plain injected EntityManager, to demonstrate raw Hibernate/JPA Session-level operations
 * (Criteria API, manual flush/refresh) alongside the declarative Spring Data methods here.
 */
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long>,
        JpaSpecificationExecutor<TradeOrder>, TradeOrderRepositoryCustom {

    boolean existsByOrderNumber(String orderNumber);
}
