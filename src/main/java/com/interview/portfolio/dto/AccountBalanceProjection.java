package com.interview.portfolio.dto;

/**
 * Interview concept - Spring Data JPA interface-based projection.
 * Used with a NATIVE SQL query (see AccountRepository#findBalanceProjectionByAccountNumber).
 * Spring Data generates a dynamic proxy at runtime that maps each getter to a column alias
 * in the SQL result set - no manual mapping code, and only the requested columns
 * are ever fetched from the DB (unlike loading full entities).
 */
public interface AccountBalanceProjection {
    String getAccountNumber();
    java.math.BigDecimal getBalance();
    String getUsername();
    Long getPortfolioCount();
}
