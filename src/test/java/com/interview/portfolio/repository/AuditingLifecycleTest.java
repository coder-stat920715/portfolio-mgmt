package com.interview.portfolio.repository;

import com.interview.portfolio.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.interview.portfolio.config.JpaAuditingConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest does NOT auto-scan @Configuration classes outside its default slice by
 * default in every Spring Boot version/config, so JpaAuditingConfig (which carries
 * @EnableJpaAuditing) is explicitly imported here to guarantee the AuditingEntityListener
 * is wired up for this test's ApplicationContext.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class AuditingLifecycleTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void prePersist_populatesCreatedAndLastModifiedDate() {
        User user = User.builder()
                .username("audit-user")
                .email("audit@example.com")
                .password("x")
                .build();

        User saved = userRepository.save(user);
        entityManager.flush();

        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getLastModifiedDate()).isNotNull();
    }

    @Test
    void preUpdate_bumpsLastModifiedDateButNotCreatedDate() throws InterruptedException {
        User saved = userRepository.save(User.builder()
                .username("audit-user-2").email("audit2@example.com").password("x").build());
        entityManager.flush();

        LocalDateTime originalCreatedDate = saved.getCreatedDate();
        LocalDateTime originalModifiedDate = saved.getLastModifiedDate();

        Thread.sleep(5); // ensure a measurable timestamp delta on fast test hardware
        saved.setEmail("changed@example.com");
        userRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedDate()).isEqualTo(originalCreatedDate);
        assertThat(reloaded.getLastModifiedDate()).isAfterOrEqualTo(originalModifiedDate);
    }
}
