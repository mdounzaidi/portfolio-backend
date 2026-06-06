package com.mdounzaidi.portfolio_backend.article.mapper;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleSummaryResponse;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import org.springframework.stereotype.Component;

@Component
public class ArticleMapper {

    public ArticleResponse buildArticleResponse(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .content(article.getContent())
                .authorName(article.getAuthorName())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .publishedAt(article.getPublishedAt())
                .articleStatus(article.getArticleStatus())
                .build();
    }

    public ArticleSummaryResponse buildArticleSummaryResponse(Article article) {
        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .authorName(article.getAuthorName())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .publishedAt(article.getPublishedAt())
                .articleStatus(article.getArticleStatus())
                .build();
    }
}
