package com.mdounzaidi.portfolio_backend.account.repository;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional <VerificationToken> findByToken(String token);

    List<VerificationToken> findByAccountAndPurposeAndUsedAtIsNullAndRevokedAtIsNull(
            Account account,
            TokenPurpose purpose
    );
}
