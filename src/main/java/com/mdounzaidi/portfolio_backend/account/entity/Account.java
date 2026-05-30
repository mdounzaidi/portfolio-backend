package com.mdounzaidi.portfolio_backend.account.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="accounts")
public class Account {

    //default feilds
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String firstName;

    private  String lastName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<AccountRole> accountRole=
            new HashSet<AccountRole>(Set.of(AccountRole.ROLE_USER));


    //safty fields
    @Builder.Default
    private boolean active=true;

    @Builder.Default
    private boolean emailVerified=false;

    @Builder.Default
    private boolean accountNonLocked=true;

    @Builder.Default
    private int failedLoginAttempts=0;

    private LocalDateTime lockedAt;
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
