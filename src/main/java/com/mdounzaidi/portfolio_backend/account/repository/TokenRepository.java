package com.mdounzaidi.portfolio_backend.account.repository;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional <VerificationToken> findByToken(String token);
}
