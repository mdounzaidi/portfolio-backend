package com.mdounzaidi.portfolio_backend.account.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountRequest {
    @NotBlank
    @Size(max = 20)
    private String firstName;

    @Size(max = 20)
    private  String lastName;

    @NotBlank
    @Size(min=8, max = 72)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "Password must contain uppercase, lowercase, number, and special character"
    )
    private String password;

    @NotBlank
    @Size(min=4,max=72)
    @Pattern(
            regexp = "^[A-Za-z0-9]+$",
            message = "Username can contain only letters and numbers"
    )
    private String username;

    @NotBlank
    @Email
    private String email;
}
