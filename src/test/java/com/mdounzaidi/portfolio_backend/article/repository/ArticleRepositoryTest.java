package com.mdounzaidi.portfolio_backend.article.repository;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void repository_shouldPersistAuthorAndFilterPublishedArticles() {
        Account author = accountRepository.saveAndFlush(account());
        articleRepository.saveAndFlush(article(author, "draft", ArticleStatus.DRAFT));
        articleRepository.saveAndFlush(article(author, "published", ArticleStatus.PUBLISHED));

        assertTrue(articleRepository.existsBySlug("published"));
        assertEquals(
                1,
                articleRepository.findByArticleStatus(
                        ArticleStatus.PUBLISHED,
                        PageRequest.of(0, 10)
                ).getTotalElements()
        );
        assertEquals(
                author.getId(),
                articleRepository.findBySlug("published").orElseThrow().getAuthor().getId()
        );
    }

    private Account account() {
        return Account.builder()
                .firstName("Test")
                .lastName("Writer")
                .username("articlewriter")
                .email("articlewriter@example.com")
                .password("encoded-password")
                .active(true)
                .emailVerified(true)
                .build();
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
}
