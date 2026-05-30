package com.mdounzaidi.portfolio_backend.account.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Invite_Details")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;

    @OneToOne
    Account inviteBy;
    @OneToOne
    VerificationToken verificationToken;

    @Column(nullable = false)
    String name;
    @Column(nullable = false)
    String email;
    LocalDateTime createdAt;
    AccountRole accountRole;
    @Builder.Default
    boolean active=true;
    @Builder.Default
    boolean accountCreated =false;
}
