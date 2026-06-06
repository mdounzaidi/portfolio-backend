package com.mdounzaidi.portfolio_backend.article.repository;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);
    Optional<Article> findBySlugAndArticleStatus(String slug, ArticleStatus articleStatus);
    boolean existsBySlug(String slug);

    Page<Article> findByArticleStatus(ArticleStatus articleStatus, Pageable pageable);

    Page<Article> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Article> findByArticleStatusAndTitleContainingIgnoreCase(
            ArticleStatus articleStatus,
            String keyword,
            Pageable pageable
    );

    Page<Article> findByAuthor(Account author, Pageable pageable);

    Page<Article> findByAuthorAndArticleStatus(
            Account author,
            ArticleStatus articleStatus,
            Pageable pageable
    );

    Page<Article> findByAuthorAndTitleContainingIgnoreCase(
            Account author,
            String keyword,
            Pageable pageable
    );

    Page<Article> findByAuthorAndArticleStatusAndTitleContainingIgnoreCase(
            Account author,
            ArticleStatus articleStatus,
            String keyword,
            Pageable pageable
    );
}
