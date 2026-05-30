package com.mdounzaidi.portfolio_backend.account.controller;

import com.mdounzaidi.portfolio_backend.account.dto.AccountRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.CredentialRequest;
import com.mdounzaidi.portfolio_backend.account.service.AccountInviteService;
import com.mdounzaidi.portfolio_backend.account.service.AccountPasswordResetService;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import com.mdounzaidi.portfolio_backend.account.service.AccountVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
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
    public ResponseEntity<AccountResponse> registerAccount(@Valid @RequestBody AccountRequest accountRequest){
        return ResponseEntity.ok(accountService.registerAccount(accountRequest));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> responseEntity( @RequestParam @NotBlank  String token){
        return ResponseEntity.ok(accountVerificationService.verifyEmail(token));
    }


    @PostMapping("/complete-invite")
    public ResponseEntity<AccountResponse> registerInviteAccount(@Valid @RequestBody  AccountRequest accountRequest, @RequestParam @NotBlank String token){
        return ResponseEntity.ok(accountInviteService.completeInvite(accountRequest, token));
    }

    @PostMapping("check-username")
    public ResponseEntity<?> checkIsUserNameAvailable(@RequestParam @NotBlank String userName){
        if(accountService.usernameExists(userName))
            return ResponseEntity.badRequest().body("UserName Not valid");
        else
            return ResponseEntity.ok("UserName Available");
    }

    @PostMapping("reset-password/generate")
    public ResponseEntity<?> resetMyPassword(@RequestParam @NotBlank String userName){
        return ResponseEntity.ok(accountPasswordReset.requestPasswordReset(userName));
    }
    @PostMapping("reset-password/update")
    public ResponseEntity<?> updateResetMyPassword(@Valid @RequestBody CredentialRequest credentialRequest,@RequestParam @NotBlank String token){
        return ResponseEntity.ok(accountPasswordReset.resetPassword(credentialRequest,token));
    }

}
