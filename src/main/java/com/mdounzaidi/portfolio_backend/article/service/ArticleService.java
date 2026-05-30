package com.mdounzaidi.portfolio_backend.article.service;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleRequest;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleSearchView;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import com.mdounzaidi.portfolio_backend.article.repository.ArticleRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public Page<ArticleSearchView> searchArticleByKeyword(String keyword, int page, int size) {
        int safeSize=Math.max(size,1);
        safeSize=Math.min(safeSize,20);
        Pageable pageable= PageRequest.of(Math.max(page,0),safeSize);

        if (keyword==null) return Page.empty(pageable);
        String trimmedKeyword=keyword.trim();

        if(trimmedKeyword.length()<2)
            return Page.empty(pageable);

        return articleRepository.findByTitleContainingIgnoreCaseAndArticleStatus(trimmedKeyword,ArticleStatus.PUBLISHED,pageable);
    }
}
