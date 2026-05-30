package com.mdounzaidi.portfolio_backend.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CredentialRequest(
        @NotBlank
        @Size(min=4,max=72)
        @Pattern(
                regexp = "^[A-Za-z0-9]+$",
                message = "Username can contain only letters and numbers"
        )
        String userName,

        @NotBlank
        @Size(min=8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must contain uppercase, lowercase, number, and special character"
        )
        String password
) {}
