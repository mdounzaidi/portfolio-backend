package com.mdounzaidi.portfolio_backend.article.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleRequest;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleUpdateRequest;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import com.mdounzaidi.portfolio_backend.article.exception.ArticleAuthorizationException;
import com.mdounzaidi.portfolio_backend.article.exception.DuplicateArticleException;
import com.mdounzaidi.portfolio_backend.article.exception.InvalidArticleStateException;
import com.mdounzaidi.portfolio_backend.article.mapper.ArticleMapper;
import com.mdounzaidi.portfolio_backend.article.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private AccountService accountService;

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService(articleRepository, articleMapper, accountService);
    }

    @Test
    void createArticle_shouldSetAuthenticatedAuthorAndGenerateUniqueSlug() {
        Account writer = account(1L, AccountRole.ROLE_WRITER);
        ArticleRequest request = ArticleRequest.builder()
                .title(" Café & Spring ")
                .content("Content")
                .build();
        when(accountService.getCurrentAccount()).thenReturn(writer);
        when(articleRepository.existsBySlug("cafe-spring")).thenReturn(true);
        when(articleRepository.existsBySlug("cafe-spring-2")).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(articleMapper.buildArticleResponse(any(Article.class))).thenReturn(new ArticleResponse());

        articleService.createArticle(request);

        verify(articleRepository).save(argThat(article ->
                article.getAuthor() == writer
                        && article.getAuthorName().equals("Test User")
                        && article.getSlug().equals("cafe-spring-2")
                        && article.getTitle().equals("Café & Spring")
        ));
    }

    @Test
    void updateArticle_shouldRegenerateSlugForOwner() {
        Account writer = account(1L, AccountRole.ROLE_WRITER);
        Article article = article(writer, ArticleStatus.DRAFT);
        when(accountService.getCurrentAccount()).thenReturn(writer);
        when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));
        when(articleRepository.existsBySlug("new-title")).thenReturn(false);
        when(articleRepository.save(article)).thenReturn(article);
        when(articleMapper.buildArticleResponse(article)).thenReturn(new ArticleResponse());

        articleService.updateArticle(
                "old-title",
                ArticleUpdateRequest.builder().title("New Title").build()
        );

        assertEquals("New Title", article.getTitle());
        assertEquals("new-title", article.getSlug());
    }

    @Test
    void updateArticle_shouldTranslateSlugConstraintConflict() {
        Account writer = account(1L, AccountRole.ROLE_WRITER);
        Article article = article(writer, ArticleStatus.DRAFT);
        when(accountService.getCurrentAccount()).thenReturn(writer);
        when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));
        when(articleRepository.existsBySlug("new-title")).thenReturn(false);
        when(articleRepository.save(article)).thenThrow(new DataIntegrityViolationException(
                "Duplicate value",
                new RuntimeException("duplicate key value violates unique constraint uk_article_slug")
        ));

        assertThrows(
                DuplicateArticleException.class,
                () -> articleService.updateArticle(
                        "old-title",
                        ArticleUpdateRequest.builder().title("New Title").build()
                )
        );
    }

    @Test
    void updateArticle_shouldRejectAnotherWritersArticle() {
        Account writer = account(1L, AccountRole.ROLE_WRITER);
        Article article = article(account(2L, AccountRole.ROLE_WRITER), ArticleStatus.DRAFT);
        when(accountService.getCurrentAccount()).thenReturn(writer);
        when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));

        assertThrows(
                ArticleAuthorizationException.class,
                () -> articleService.updateArticle(
                        "old-title",
                        ArticleUpdateRequest.builder().content("Updated").build()
                )
        );

        verify(articleRepository, never()).save(any());
    }

    @Test
    void publishArticle_shouldPublishDraft() {
        Article article = article(account(1L, AccountRole.ROLE_WRITER), ArticleStatus.DRAFT);
        when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));
        when(articleRepository.save(article)).thenReturn(article);
        when(articleMapper.buildArticleResponse(article)).thenReturn(new ArticleResponse());

        articleService.publishArticle("old-title");

        assertEquals(ArticleStatus.PUBLISHED, article.getArticleStatus());
        assertNotNull(article.getPublishedAt());
    }

    @Test
    void publishArticle_shouldRejectPublishedArticle() {
        Article article = article(account(1L, AccountRole.ROLE_WRITER), ArticleStatus.PUBLISHED);
        when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));

        assertThrows(
                InvalidArticleStateException.class,
                () -> articleService.publishArticle("old-title")
        );
    }

    @Test
    void deleteArticle_shouldRejectPublishedArticleForWriter() {
        Account writer = account(1L, AccountRole.ROLE_WRITER);
        Article article = article(writer, ArticleStatus.PUBLISHED);
        when(accountService.getCurrentAccount()).thenReturn(writer);
        when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));

        assertThrows(
                ArticleAuthorizationException.class,
                () -> articleService.deleteArticle("old-title")
        );

        verify(articleRepository, never()).delete(any());
    }

    private Account account(long id, AccountRole role) {
        Account account = Account.builder()
                .id(id)
                .firstName("Test")
                .lastName("User")
                .username("writer" + id)
                .build();
        account.setAccountRole(new HashSet<>(Set.of(role)));
        return account;
    }

    private Article article(Account author, ArticleStatus status) {
        return Article.builder()
                .id(10L)
                .title("Old Title")
                .slug("old-title")
                .content("Content")
                .author(author)
                .authorName("Test User")
                .articleStatus(status)
                .build();
    }
}
