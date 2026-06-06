package com.mdounzaidi.portfolio_backend.account.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.InviteRepository;
import com.mdounzaidi.portfolio_backend.account.repository.ResetPassRepository;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSecurityIntegrationTest {

    private static final String RAW_PASSWORD = "StrongPass@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private ResetPassRepository resetPassRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        inviteRepository.deleteAll();
        resetPassRepository.deleteAll();
        tokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void login_shouldReturnJwt_whenCredentialsAreValid() throws Exception {
        account("testuser", "test@example.com", AccountRole.ROLE_USER);

        mockMvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("testuser", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.account.username").value("testuser"));
    }

    @Test
    void login_shouldReturnJwt_whenEmailAddressIsUsedAsIdentifier() throws Exception {
        account("testuser", "test@example.com", AccountRole.ROLE_USER);

        mockMvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(" TEST@EXAMPLE.COM ", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.account.username").value("testuser"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenPasswordIsInvalid() throws Exception {
        account("testuser", "test@example.com", AccountRole.ROLE_USER);

        mockMvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("testuser", "WrongPass@123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void getMyDetails_shouldReturnUnauthorized_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void getMyDetails_shouldReturnUnauthorized_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void getMyDetails_shouldReturnCurrentAccount_whenJwtIsValid() throws Exception {
        Account account = account("testuser", "test@example.com", AccountRole.ROLE_USER);
        String accessToken = jwtTokenService.generateAccessToken(account);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void inviteAccountUser_shouldReturnForbidden_whenJwtHasUserRole() throws Exception {
        Account account = account("testuser", "test@example.com", AccountRole.ROLE_USER);
        String accessToken = jwtTokenService.generateAccessToken(account);

        mockMvc.perform(post("/api/auth/invites")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequest()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void inviteAccountUser_shouldReturnAccepted_whenJwtHasAdminRole() throws Exception {
        Account account = account("adminuser", "admin@example.com", AccountRole.ROLE_ADMIN);
        String accessToken = jwtTokenService.generateAccessToken(account);

        mockMvc.perform(post("/api/auth/invites")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequest()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("we have sent invitation link"));
    }

    private Account account(String username, String email, AccountRole... roles) {
        Account account = Account.builder()
                .firstName("Test")
                .lastName("User")
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .active(true)
                .emailVerified(true)
                .build();
        account.setAccountRole(new HashSet<>(Set.of(roles)));
        return accountRepository.saveAndFlush(account);
    }

    private String loginRequest(String identifier, String password) {
        return """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(identifier, password);
    }

    private String inviteRequest() {
        return """
                {
                  "accountRole": "ROLE_WRITER",
                  "name": "Invited User",
                  "email": "invited@example.com"
                }
                """;
    }
}
