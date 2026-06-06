package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.repository.InviteRepository;
import com.mdounzaidi.portfolio_backend.account.repository.ResetPassRepository;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AccountTokenCleanupService {

    private final TokenRepository tokenRepository;
    private final ResetPassRepository resetPassRepository;
    private final InviteRepository inviteRepository;

    @Scheduled(cron = "${app.cleanup.tokens.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldRecordCutoff = now.minusDays(7);

        resetPassRepository.deleteByActiveFalseAndCreatedAtBefore(oldRecordCutoff);
        inviteRepository.deleteByActiveFalseAndCreatedAtBefore(oldRecordCutoff);

        resetPassRepository.deleteByVerificationToken_ExpireAtBefore(now);
        inviteRepository.deleteByVerificationToken_ExpireAtBefore(now);

        tokenRepository.deleteByExpireAtBefore(now);
    }
}
