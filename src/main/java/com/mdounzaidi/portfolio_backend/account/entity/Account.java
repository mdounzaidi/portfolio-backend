package com.mdounzaidi.portfolio_backend.account.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_username", columnList = "username"),
                @Index(name = "idx_accounts_email", columnList = "email")
        }
)
public class Account {

    //default feilds
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private  String lastName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<AccountRole> accountRole =
            new HashSet<>(Set.of(AccountRole.ROLE_USER));



    //safety fields
    @Builder.Default
    @Column(nullable = false)
    private boolean active=true;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified=false;

    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked=true;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts=0;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;


    @PrePersist
    public void onCreate(){
        createdAt=LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate(){
        updatedAt=LocalDateTime.now();
    }

}
