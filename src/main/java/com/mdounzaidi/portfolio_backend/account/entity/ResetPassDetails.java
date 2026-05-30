package com.mdounzaidi.portfolio_backend.account.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ResetPass_Details")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPassDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;
    @OneToOne
    Account account;
    @OneToOne
    VerificationToken verificationToken;

    @Column(updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Builder.Default
    boolean active=true;
}
