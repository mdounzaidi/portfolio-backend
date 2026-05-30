package com.mdounzaidi.portfolio_backend.account.repository;

import com.mdounzaidi.portfolio_backend.account.entity.InviteDetails;
import com.mdounzaidi.portfolio_backend.account.entity.ResetPassDetails;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResetPassRepository extends JpaRepository<ResetPassDetails, Long> {
    Optional <ResetPassDetails> findByVerificationToken(VerificationToken verificationToken);
}
