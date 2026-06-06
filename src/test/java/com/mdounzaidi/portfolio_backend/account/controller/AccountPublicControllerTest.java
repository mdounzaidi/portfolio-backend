package com.mdounzaidi.portfolio_backend.account.controller;

import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.exception.AccountExceptionHandler;
import com.mdounzaidi.portfolio_backend.account.exception.DuplicateAccountException;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidTokenException;
import com.mdounzaidi.portfolio_backend.account.service.AccountAuthService;
import com.mdounzaidi.portfolio_backend.account.service.AccountInviteService;
import com.mdounzaidi.portfolio_backend.account.service.AccountPasswordResetService;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import com.mdounzaidi.portfolio_backend.account.service.AccountVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountPublicControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountPasswordResetService accountPasswordResetService;

    @Mock
    private AccountVerificationService accountVerificationService;

    @Mock
    private AccountInviteService accountInviteService;

    @Mock
    private AccountAuthService accountAuthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountPublicController(
                        accountService,
                        accountPasswordResetService,
                        accountVerificationService,
                        accountInviteService,
                        accountAuthService
                ))
                .setControllerAdvice(new AccountExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerAccount_shouldReturnAccountResponse() throws Exception {
        when(accountService.registerAccount(any()))
                .thenReturn(accountResponse("testuser", "test@example.com", false));

        mockMvc.perform(post("/api/public/register")
                        .contentType("application/json")
                        .content(validAccountRequest("testuser", "test@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void registerAccount_shouldReturnBadRequest_whenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/public/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "User",
                                  "username": "ab",
                                  "email": "not-an-email",
                                  "password": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));

        verify(accountService, never()).registerAccount(any());
    }

    @Test
    void registerAccount_shouldReturnConflict_whenAccountAlreadyExists() throws Exception {
        when(accountService.registerAccount(any()))
                .thenThrow(new DuplicateAccountException("Username already exists"));

        mockMvc.perform(post("/api/public/register")
                        .contentType("application/json")
                        .content(validAccountRequest("testuser", "test@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate account"));
    }

    @Test
    void verify_shouldReturnSuccessMessage() throws Exception {
        when(accountVerificationService.verifyEmail("raw-token")).thenReturn("Account Verified");

        mockMvc.perform(get("/api/public/verify")
                        .param("token", "raw-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account Verified"));
    }

    @Test
    void verify_shouldReturnBadRequest_whenTokenIsInvalid() throws Exception {
        when(accountVerificationService.verifyEmail("bad-token"))
                .thenThrow(new InvalidTokenException("Invalid token"));

        mockMvc.perform(get("/api/public/verify")
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid token"));
    }

    @Test
    void resendVerificationEmail_shouldReturnAcceptedGenericMessage() throws Exception {
        when(accountVerificationService.resendVerificationEmail("testuser"))
                .thenReturn("If an unverified account exists, a verification email will be sent.");

        mockMvc.perform(post("/api/public/verification/resend")
                        .param("identifier", "testuser"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If an unverified account exists, a verification email will be sent."));
    }

    @Test
    void checkUsername_shouldReturnBadRequest_whenUsernameExists() throws Exception {
        when(accountService.usernameExists("testuser")).thenReturn(true);

        mockMvc.perform(post("/api/public/check-username")
                        .param("userName", "testuser"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is not available"));
    }

    @Test
    void checkUsername_shouldReturnOk_whenUsernameIsAvailable() throws Exception {
        when(accountService.usernameExists("newuser")).thenReturn(false);

        mockMvc.perform(get("/api/public/usernames/newuser/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Username is available"));
    }

    @Test
    void requestPasswordReset_shouldReturnGenericMessage() throws Exception {
        when(accountPasswordResetService.requestPasswordReset("testuser"))
                .thenReturn("If an account exists, a password reset email will be sent.");

        mockMvc.perform(post("/api/public/reset-password/generate")
                        .param("userName", "testuser"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If an account exists, a password reset email will be sent."));
    }

    @Test
    void resetPassword_shouldReturnNoContent() throws Exception {
        when(accountPasswordResetService.resetPassword(any(), eq("raw-token")))
                .thenReturn("Password Reset Done");

        mockMvc.perform(post("/api/public/password-resets/confirm")
                        .param("token", "raw-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userName": "testuser",
                                  "password": "StrongPass@123"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void completeInvite_shouldReturnCreatedAccountResponse() throws Exception {
        when(accountInviteService.completeInvite(any(), eq("invite-token")))
                .thenReturn(accountResponse("inviteduser", "invited@example.com", true));

        mockMvc.perform(post("/api/public/invites/complete")
                        .param("token", "invite-token")
                        .contentType("application/json")
                        .content(validAccountRequest("inviteduser", "invited@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("inviteduser"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    private String validAccountRequest(String username, String email) {
        return """
                {
                  "firstName": "Test",
                  "lastName": "User",
                  "username": "%s",
                  "email": "%s",
                  "password": "StrongPass@123"
                }
                """.formatted(username, email);
    }

    private AccountResponse accountResponse(String username, String email, boolean verified) {
        return AccountResponse.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .username(username)
                .email(email)
                .emailVerified(verified)
                .build();
    }
}
