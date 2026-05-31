package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;

public record GeneratedToken(
        String rawToken,
        VerificationToken verificationToken
) {
}
