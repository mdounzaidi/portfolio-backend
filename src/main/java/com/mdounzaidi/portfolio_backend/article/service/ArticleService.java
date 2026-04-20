package com.mdounzaidi.portfolio_backend.article.service;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleRequest;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import com.mdounzaidi.portfolio_backend.article.repository.ArticleRepository;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public ArticleResponse createArticle(ArticleRequest articleRequest) {

        String slug= articleRequest.getTitle()
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", "-");
        var now = java.time.LocalDateTime.now();

        Article article= Article.builder().
                content(articleRequest.getContent()).
                title(articleRequest.getTitle()).
                authorName(articleRequest.getAuthorName()).
                slug(slug).
                updatedAt(now).
                createdAt(now).
                build();

        Article savedArticle= articleRepository.save(article);
        return makeArticleResponse(savedArticle);

    }

    public ArticleResponse makeArticleResponse(Article article){
        return  ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .authorName(article.getAuthorName())
                .updatedAt(article.getUpdatedAt())
                .slug(article.getSlug())
                .articleStatus(article.getArticleStatus())
                .build();
    }

    public ArticleResponse findSlugForAdmin(String slug){
        Article article=articleRepository.findBySlug(slug)
                .orElseThrow(
                        ()-> new RuntimeException("no article find by slug name")
                );
        return makeArticleResponse(article);
    }

    public ArticleResponse findSlugForPublic(String slug) {
        Article article =articleRepository.findBySlugAndArticleStatus(slug, ArticleStatus.PUBLISHED)
                .orElseThrow(
                        ()-> new RuntimeException("no article find by slug name")
                );
        return makeArticleResponse(article);
    }
}
