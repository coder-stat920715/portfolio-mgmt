package com.interview.portfolio.repository;

import com.interview.portfolio.entity.TradeOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Naming convention "<RepositoryName>Impl" is mandatory - Spring Data JPA auto-detects
 * this class as the custom implementation backing TradeOrderRepositoryCustom and merges
 * it into the TradeOrderRepository proxy alongside the generated Spring Data methods.
 *
 * @PersistenceContext-injected EntityManager gives direct access to the underlying
 * Hibernate Session (via entityManager.unwrap(Session.class) if raw Hibernate API is
 * needed) - this is the lowest-level entry point Spring Data itself is built upon.
 */
@Repository
public class TradeOrderRepositoryImpl implements TradeOrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<TradeOrder> findExecutedOrdersAboveValue(BigDecimal minOrderValue) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TradeOrder> query = cb.createQuery(TradeOrder.class);
        Root<TradeOrder> root = query.from(TradeOrder.class);

        // orderValue = quantity * price -> expressed as a computed Criteria expression
        Expression<BigDecimal> orderValue = cb.prod(root.get("quantity"), root.get("price"));

        Predicate executedPredicate = cb.equal(root.get("status"), TradeOrder.OrderStatus.EXECUTED);
        Predicate valuePredicate = cb.greaterThanOrEqualTo(orderValue, minOrderValue);

        query.select(root)
                .where(cb.and(executedPredicate, valuePredicate))
                .orderBy(cb.desc(root.get("createdDate")));

        return entityManager.createQuery(query).getResultList();
    }
}
