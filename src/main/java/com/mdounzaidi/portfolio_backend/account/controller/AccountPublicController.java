package com.mdounzaidi.portfolio_backend.account.controller;

import com.mdounzaidi.portfolio_backend.account.dto.AccountRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.CredentialRequest;
import com.mdounzaidi.portfolio_backend.account.service.AccountInviteService;
import com.mdounzaidi.portfolio_backend.account.service.AccountPasswordResetService;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import com.mdounzaidi.portfolio_backend.account.service.AccountVerificationService;
import com.mdounzaidi.portfolio_backend.common.dto.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/public")
@AllArgsConstructor
@Validated
public class AccountPublicController {

    private final AccountService accountService;
    private final AccountPasswordResetService accountPasswordReset;
    private final AccountVerificationService accountVerificationService;
    private final AccountInviteService accountInviteService;

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> registerAccount(@Valid @RequestBody AccountRequest accountRequest) {
        AccountResponse response = accountService.registerAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verifyAccount(@RequestParam @NotBlank String token) {
        return ResponseEntity.ok(new MessageResponse(accountVerificationService.verifyEmail(token)));
    }

    @PostMapping({"/complete-invite", "/invites/complete"})
    public ResponseEntity<AccountResponse> registerInviteAccount(
            @Valid @RequestBody AccountRequest accountRequest,
            @RequestParam @NotBlank String token
    ) {
        AccountResponse response = accountInviteService.completeInvite(accountRequest, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/usernames/{userName}/availability")
    public ResponseEntity<MessageResponse> checkIsUserNameAvailableByPath(@PathVariable @NotBlank String userName) {
        return buildUsernameAvailabilityResponse(userName);
    }

    @PostMapping("check-username")
    public ResponseEntity<MessageResponse> checkIsUserNameAvailable(@RequestParam @NotBlank String userName) {
        return buildUsernameAvailabilityResponse(userName);
    }

    @PostMapping("password-resets")
    public ResponseEntity<MessageResponse> requestPasswordReset(@RequestParam @NotBlank String userName) {
        return buildPasswordResetResponse(userName);
    }

    @PostMapping("reset-password/generate")
    public ResponseEntity<MessageResponse> resetMyPassword(@RequestParam @NotBlank String userName) {
        return buildPasswordResetResponse(userName);
    }

    @PostMapping({"password-resets/confirm", "reset-password/update"})
    public ResponseEntity<Void> updateResetMyPassword(
            @Valid @RequestBody CredentialRequest credentialRequest,
            @RequestParam @NotBlank String token
    ) {
        accountPasswordReset.resetPassword(credentialRequest, token);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<MessageResponse> buildUsernameAvailabilityResponse(String userName) {
        if (accountService.usernameExists(userName)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("Username is not available"));
        }

        return ResponseEntity.ok(new MessageResponse("Username is available"));
    }

    private ResponseEntity<MessageResponse> buildPasswordResetResponse(String userName) {
        return ResponseEntity
                .accepted()
                .body(new MessageResponse(accountPasswordReset.requestPasswordReset(userName)));
    }
}
