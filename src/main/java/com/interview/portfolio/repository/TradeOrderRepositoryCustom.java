package com.interview.portfolio.repository;

import com.interview.portfolio.entity.TradeOrder;

import java.math.BigDecimal;
import java.util.List;

public interface TradeOrderRepositoryCustom {

    /**
     * Hand-rolled Criteria API query (as opposed to Spring Data derived queries or JPQL
     * strings) - useful in interviews to show understanding of the underlying
     * CriteriaBuilder/CriteriaQuery/Root/Predicate API that Spring Data Specifications
     * are themselves built on top of.
     */
    List<TradeOrder> findExecutedOrdersAboveValue(BigDecimal minOrderValue);
}
