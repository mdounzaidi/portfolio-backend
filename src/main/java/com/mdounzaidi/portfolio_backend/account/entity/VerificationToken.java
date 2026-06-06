package com.mdounzaidi.portfolio_backend.account.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(
        schema = "account",
        name = "verification_token",
        indexes = {
                @Index(name = "idx_verification_token_account_purpose", columnList = "account_id, purpose"),
                @Index(name = "idx_verification_token_expire_at", columnList = "expire_at"),
                @Index(name = "idx_verification_token_lifecycle", columnList = "used_at, revoked_at")
        }
)
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }


}
