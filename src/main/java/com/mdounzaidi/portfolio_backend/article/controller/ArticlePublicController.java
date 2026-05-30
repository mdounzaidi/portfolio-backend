package com.mdounzaidi.portfolio_backend.article.controller;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleSearchView;
import com.mdounzaidi.portfolio_backend.article.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/public/articles")
public class ArticlePublicController {

    private final ArticleService articleService;

    public ArticlePublicController(ArticleService articleService) {
        this.articleService = articleService;
    }


    @GetMapping("/{slug}")
    public ResponseEntity<ArticleResponse> getBySlugForPublic(@PathVariable String slug){
        return ResponseEntity.ok(articleService.findSlugForPublic(slug));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleSearchView>> findArticlesByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(
                articleService.searchArticleByKeyword(keyword, page, size)
        );
    }
}
