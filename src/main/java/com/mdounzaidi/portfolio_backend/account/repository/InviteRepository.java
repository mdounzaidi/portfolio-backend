package com.mdounzaidi.portfolio_backend.account.repository;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.InviteDetails;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<InviteDetails, Long> {
    Optional <InviteDetails> findByVerificationToken(VerificationToken verificationToken);
    Optional <InviteDetails> findByEmail(String email);

    long deleteByActiveFalseAndCreatedAtBefore(LocalDateTime cutoff);

    long deleteByVerificationToken_ExpireAtBefore(LocalDateTime cutoff);
}
