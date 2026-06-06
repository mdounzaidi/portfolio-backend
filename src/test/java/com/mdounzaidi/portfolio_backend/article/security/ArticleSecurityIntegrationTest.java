package com.mdounzaidi.portfolio_backend.article.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.security.JwtTokenService;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import com.mdounzaidi.portfolio_backend.article.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createArticle_shouldReturnForbiddenForUserAndCreatedForWriter() throws Exception {
        Account user = account("user", AccountRole.ROLE_USER);
        Account writer = account("writer", AccountRole.ROLE_WRITER);

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", bearer(user))
                        .contentType("application/json")
                        .content(articleRequest("User Article")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content(articleRequest("Writer Article")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("writer-article"))
                .andExpect(jsonPath("$.authorName").value("Test Writer"));
    }

    @Test
    void publicEndpoint_shouldHideDraftAndReturnPublishedArticle() throws Exception {
        Account writer = account("writer", AccountRole.ROLE_WRITER);
        articleRepository.saveAndFlush(article(writer, "draft", ArticleStatus.DRAFT));
        articleRepository.saveAndFlush(article(writer, "published", ArticleStatus.PUBLISHED));

        mockMvc.perform(get("/api/public/articles/draft"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/public/articles/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("published"));
    }

    @Test
    void writer_shouldNotUpdateAnotherWritersArticle() throws Exception {
        Account firstWriter = account("writerone", AccountRole.ROLE_WRITER);
        Account secondWriter = account("writertwo", AccountRole.ROLE_WRITER);
        articleRepository.saveAndFlush(article(firstWriter, "owned-article", ArticleStatus.DRAFT));

        mockMvc.perform(patch("/api/articles/owned-article")
                        .header("Authorization", bearer(secondWriter))
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "Updated"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    private Account account(String username, AccountRole role) {
        Account account = Account.builder()
                .firstName("Test")
                .lastName("Writer")
                .username(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode("StrongPass@123"))
                .active(true)
                .emailVerified(true)
                .build();
        account.setAccountRole(new HashSet<>(Set.of(role)));
        return accountRepository.saveAndFlush(account);
    }

    private Article article(Account author, String slug, ArticleStatus status) {
        return Article.builder()
                .title(slug)
                .slug(slug)
                .content("Content")
                .author(author)
                .authorName("Test Writer")
                .articleStatus(status)
                .build();
    }

    private String bearer(Account account) {
        return "Bearer " + jwtTokenService.generateAccessToken(account);
    }

    private String articleRequest(String title) {
        return """
                {
                  "title": "%s",
                  "content": "Content"
                }
                """.formatted(title);
    }
}
