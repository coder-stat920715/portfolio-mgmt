package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Root aggregate of the "ownership" side of the domain.
 *
 * Interview concept: bidirectional @OneToMany/@ManyToOne.
 *  - The Account side (@ManyToOne) owns the foreign key (accounts.user_id) and is the
 *    "owning side" of the relationship - it is what Hibernate reads to determine the FK value.
 *  - The User side (@OneToMany mappedBy = "user") is the "inverse/non-owning side" - purely
 *    for object-graph navigation, it does NOT generate SQL for the FK by itself.
 *  - cascade = CascadeType.ALL + orphanRemoval = true means: deleting a User deletes all
 *    their Accounts, and removing an Account from user.getAccounts() deletes it from the DB too.
 *  - Helper methods (addAccount/removeAccount) keep BOTH sides of the association in sync,
 *    which is critical: forgetting to sync the inverse side is one of the most common
 *    Hibernate bugs in interviews and in production.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "accounts") // avoid infinite recursion / lazy-init exceptions in toString
@EqualsAndHashCode(of = "id")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    /**
     * FetchType.LAZY is used consistently across the codebase (see project-wide rule).
     * Spring Boot 2.x+/Hibernate default for @OneToMany IS already LAZY, but it's declared
     * explicitly here for clarity and interview visibility.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    public void addAccount(Account account) {
        accounts.add(account);
        account.setUser(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setUser(null);
    }

    public enum UserRole {
        CUSTOMER, ADVISOR, ADMIN
    }
}
