package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.repository.InviteRepository;
import com.mdounzaidi.portfolio_backend.account.repository.ResetPassRepository;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountTokenCleanupServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private ResetPassRepository resetPassRepository;

    @Mock
    private InviteRepository inviteRepository;

    private AccountTokenCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new AccountTokenCleanupService(
                tokenRepository,
                resetPassRepository,
                inviteRepository
        );
    }

    @Test
    void cleanupExpiredTokens_shouldDeleteOldInactiveRowsThenExpiredDependentRowsThenTokens() {
        cleanupService.cleanupExpiredTokens();

        InOrder inOrder = inOrder(resetPassRepository, inviteRepository, tokenRepository);
        inOrder.verify(resetPassRepository).deleteByActiveFalseAndCreatedAtBefore(org.mockito.ArgumentMatchers.any());
        inOrder.verify(inviteRepository).deleteByActiveFalseAndCreatedAtBefore(org.mockito.ArgumentMatchers.any());
        inOrder.verify(resetPassRepository).deleteByVerificationToken_ExpireAtBefore(org.mockito.ArgumentMatchers.any());
        inOrder.verify(inviteRepository).deleteByVerificationToken_ExpireAtBefore(org.mockito.ArgumentMatchers.any());
        inOrder.verify(tokenRepository).deleteByExpireAtBefore(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cleanupExpiredTokens_shouldUseSevenDayCutoffForInactiveResetAndInviteRows() {
        ArgumentCaptor<LocalDateTime> resetCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> inviteCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        cleanupService.cleanupExpiredTokens();

        verify(resetPassRepository).deleteByActiveFalseAndCreatedAtBefore(resetCutoffCaptor.capture());
        verify(inviteRepository).deleteByActiveFalseAndCreatedAtBefore(inviteCutoffCaptor.capture());

        LocalDateTime sixDaysAgo = LocalDateTime.now().minusDays(6);
        assertTrue(resetCutoffCaptor.getValue().isBefore(sixDaysAgo));
        assertTrue(inviteCutoffCaptor.getValue().isBefore(sixDaysAgo));
    }

    @Test
    void cleanupExpiredTokens_shouldUseCurrentTimeForExpiredTokenCleanup() {
        ArgumentCaptor<LocalDateTime> tokenCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        cleanupService.cleanupExpiredTokens();

        verify(tokenRepository).deleteByExpireAtBefore(tokenCutoffCaptor.capture());

        assertTrue(tokenCutoffCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}
