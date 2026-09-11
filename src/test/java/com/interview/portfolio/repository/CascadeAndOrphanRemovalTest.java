package com.interview.portfolio.repository;

import com.interview.portfolio.entity.Account;
import com.interview.portfolio.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest boots ONLY the JPA slice (embedded H2 by default, repositories, entity
 * manager) - no web layer, no full ApplicationContext - making these tests fast and
 * focused purely on the persistence layer's behavior. Each test method runs in an
 * implicit transaction that's rolled back afterward, keeping tests isolated.
 *
 * @AutoConfigureTestDatabase(replace = NONE) keeps OUR application-test.yml datasource
 * and Hibernate properties (incl. L2 cache config) instead of Spring Boot's default
 * behavior of silently swapping in an anonymous embedded database with default settings.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CascadeAndOrphanRemovalTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("jdoe")
                .email("jdoe@example.com")
                .password("hashed-password")
                .build();
    }

    @Test
    void cascadePersist_savesAccountsWhenUserIsSaved() {
        Account account = Account.builder()
                .accountNumber("ACC-1001")
                .balance(new BigDecimal("1000.00"))
                .build();
        user.addAccount(account); // keeps both sides of the bidirectional association in sync

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear(); // force a fresh read from the DB, bypassing the 1st-level cache

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAccounts()).hasSize(1);
        assertThat(reloaded.getAccounts().get(0).getAccountNumber()).isEqualTo("ACC-1001");
    }

    @Test
    void orphanRemoval_deletesAccountWhenRemovedFromUserCollection() {
        Account account = Account.builder()
                .accountNumber("ACC-2002")
                .balance(new BigDecimal("500.00"))
                .build();
        user.addAccount(account);
        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        Account toRemove = reloaded.getAccounts().get(0);

        // orphanRemoval = true means simply detaching this child from the parent
        // collection is enough to trigger a DELETE on flush - no explicit
        // accountRepository.delete(...) call is required.
        reloaded.removeAccount(toRemove);
        userRepository.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        assertThat(accountRepository.findById(toRemove.getId())).isEmpty();
    }

    @Test
    void cascadeDelete_deletingUserDeletesAllAccounts() {
        Account acc1 = Account.builder().accountNumber("ACC-3003").balance(BigDecimal.TEN).build();
        Account acc2 = Account.builder().accountNumber("ACC-3004").balance(BigDecimal.ONE).build();
        user.addAccount(acc1);
        user.addAccount(acc2);
        User saved = userRepository.save(user);
        entityManager.flush();

        Long acc1Id = acc1.getId();
        Long acc2Id = acc2.getId();

        userRepository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(accountRepository.findById(acc1Id)).isEmpty();
        assertThat(accountRepository.findById(acc2Id)).isEmpty();
    }
}
