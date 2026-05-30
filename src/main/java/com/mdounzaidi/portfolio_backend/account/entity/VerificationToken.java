package com.mdounzaidi.portfolio_backend.account.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "verification_token")
public class VerificationToken {
    @Column(unique = true)
    String token;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;
    @OneToOne
    Account account;
    LocalDateTime expireAt;

}
