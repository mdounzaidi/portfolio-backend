package com.mdounzaidi.portfolio_backend.article.controller;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleSummaryResponse;
import com.mdounzaidi.portfolio_backend.article.service.ArticleService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/public/articles")
@Validated
public class ArticlePublicController {

    private final ArticleService articleService;

    public ArticlePublicController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ResponseEntity<Page<ArticleSummaryResponse>> findPublishedArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(articleService.findPublishedArticles(page, size));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleResponse> getBySlugForPublic(@PathVariable @NotBlank String slug){
        return ResponseEntity.ok(articleService.findSlugForPublic(slug));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleSummaryResponse>> findArticlesByKeyword(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(
                articleService.searchArticleByKeyword(keyword, page, size)
        );
    }
}
