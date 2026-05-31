package com.mdounzaidi.portfolio_backend.account.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "reset_pass_details",
        indexes = {
                @Index(name = "idx_reset_pass_account_active", columnList = "account_id, active"),
                @Index(name = "idx_reset_pass_token", columnList = "verification_token_id")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPassDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToOne
    @JoinColumn(name = "verification_token_id", nullable = false)
    private VerificationToken verificationToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
