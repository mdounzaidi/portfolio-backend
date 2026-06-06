package com.mdounzaidi.portfolio_backend.article.mapper;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleSummaryResponse;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleMapperTest {

    private final ArticleMapper articleMapper = new ArticleMapper();

    @Test
    void buildResponses_shouldMapArticleFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now().minusHours(1);
        LocalDateTime publishedAt = LocalDateTime.now();
        Article article = Article.builder()
                .id(10L)
                .title("Test Article")
                .slug("test-article")
                .content("Content")
                .authorName("Test Writer")
                .articleStatus(ArticleStatus.PUBLISHED)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .publishedAt(publishedAt)
                .build();

        ArticleResponse response = articleMapper.buildArticleResponse(article);
        ArticleSummaryResponse summary = articleMapper.buildArticleSummaryResponse(article);

        assertEquals("Content", response.getContent());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(publishedAt, response.getPublishedAt());
        assertEquals(10L, summary.getId());
        assertEquals("test-article", summary.getSlug());
        assertEquals(ArticleStatus.PUBLISHED, summary.getArticleStatus());
    }
}
