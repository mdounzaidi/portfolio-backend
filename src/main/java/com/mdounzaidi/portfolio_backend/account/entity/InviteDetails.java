package com.mdounzaidi.portfolio_backend.account.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        schema = "account",
        name = "invite_details",
        indexes = {
                @Index(name = "idx_invite_details_email", columnList = "email"),
                @Index(name = "idx_invite_details_token", columnList = "verification_token_id"),
                @Index(name = "idx_invite_details_inviter", columnList = "invited_by_account_id"),
                @Index(name = "idx_invite_details_state", columnList = "active, account_created")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invited_by_account_id", nullable = false)
    private Account inviteBy;

    @OneToOne
    @JoinColumn(name = "verification_token_id", nullable = false)
    private VerificationToken verificationToken;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", nullable = false)
    private AccountRole accountRole;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "account_created", nullable = false)
    private boolean accountCreated = false;
}
