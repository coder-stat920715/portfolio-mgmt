package com.interview.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Entry point for the Financial Portfolio & Order Management System.
 *
 * Interview note: @EnableTransactionManagement is technically auto-configured by
 * Spring Boot already, but is declared explicitly here to make the transaction
 * infrastructure visible for discussion (PlatformTransactionManager / JpaTransactionManager).
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.interview.portfolio.repository")
@EnableTransactionManagement
public class PortfolioMgmtApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioMgmtApplication.class, args);
    }
}
