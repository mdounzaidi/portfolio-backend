package com.mdounzaidi.portfolio_backend.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountUpdateRequest {

    @Size(max = 20)
    private String firstName;
    @Size(max = 20)
    private  String lastName;

    @Size(min=4,max=72)
    @Pattern(
            regexp = "^[A-Za-z0-9]+$",
            message = "Username can contain only letters and numbers"
    )
    private String username;

    @Email
    private String email;
}
