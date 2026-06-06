package com.mdounzaidi.portfolio_backend.account.controller;

import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.exception.AccountAuthorizationException;
import com.mdounzaidi.portfolio_backend.account.exception.AccountExceptionHandler;
import com.mdounzaidi.portfolio_backend.account.service.AccountInviteService;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountAuthControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountInviteService accountInviteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountAuthController(accountService, accountInviteService))
                .setControllerAdvice(new AccountExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void getMyDetails_shouldReturnCurrentAccount() throws Exception {
        when(accountService.getCurrentAccountDetails())
                .thenReturn(accountResponse("testuser", "test@example.com", true));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateMyAccount_shouldReturnUpdatedAccount() throws Exception {
        when(accountService.updateCurrentAccount(any()))
                .thenReturn(accountResponse("updateduser", "updated@example.com", false));

        mockMvc.perform(patch("/api/auth/me")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "lastName": "User",
                                  "username": "updateduser",
                                  "email": "updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void updateMyAccount_shouldReturnBadRequest_whenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/update")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "bad user",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));

        verify(accountService, never()).updateCurrentAccount(any());
    }

    @Test
    void updatePassword_shouldReturnNoContent() throws Exception {
        when(accountService.changePassword(any())).thenReturn("password updated");

        mockMvc.perform(patch("/api/auth/me/password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "oldPassword": "OldPass@123",
                                  "newPassword": "NewPass@123"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatePassword_shouldReturnNoContent_whenOldPasswordIsWeakButNewPasswordIsStrong() throws Exception {
        when(accountService.changePassword(any())).thenReturn("password updated");

        mockMvc.perform(patch("/api/auth/me/password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "oldPassword": "weak",
                                  "newPassword": "NewPass@123"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatePassword_shouldReturnBadRequest_whenPasswordIsWeak() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "oldPassword": "weak",
                                  "newPassword": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));

        verify(accountService, never()).changePassword(any());
    }

    @Test
    void inviteAccountUser_shouldReturnSuccessMessage() throws Exception {
        when(accountInviteService.createInvite(any())).thenReturn("we have sent invitation link");

        mockMvc.perform(post("/api/auth/invites")
                        .contentType("application/json")
                        .content("""
                                {
                                  "accountRole": "ROLE_WRITER",
                                  "name": "Invited User",
                                  "email": "invited@example.com"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("we have sent invitation link"));
    }

    @Test
    void inviteAccountUser_shouldReturnForbidden_whenCurrentAccountCannotInviteRole() throws Exception {
        when(accountInviteService.createInvite(any()))
                .thenThrow(new AccountAuthorizationException("You are not authorized to invite this account role"));

        mockMvc.perform(post("/api/auth/invite")
                        .contentType("application/json")
                        .content("""
                                {
                                  "accountRole": "ROLE_WRITER",
                                  "name": "Invited User",
                                  "email": "invited@example.com"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void inviteAccountUser_shouldReturnBadRequest_whenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/invite")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));

        verify(accountInviteService, never()).createInvite(any());
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
