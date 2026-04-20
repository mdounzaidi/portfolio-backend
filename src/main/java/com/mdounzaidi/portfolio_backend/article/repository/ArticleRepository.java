package com.mdounzaidi.portfolio_backend.article.repository;

import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);
    Optional<Article> findBySlugAndArticleStatus(String slug, ArticleStatus articleStatus);
}
