package com.mdounzaidi.portfolio_backend.account.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AccountResponse account
) {
    public static LoginResponse bearer(
            String accessToken,
            long expiresIn,
            AccountResponse account
    ) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, account);
    }
}
