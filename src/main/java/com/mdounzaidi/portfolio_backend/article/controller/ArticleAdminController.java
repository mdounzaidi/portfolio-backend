package com.mdounzaidi.portfolio_backend.article.controller;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleRequest;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/articles")
public class ArticleAdminController {

    private final ArticleService articleService;

    public ArticleAdminController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping()
    public ResponseEntity<ArticleResponse> createArticle(@RequestBody ArticleRequest articleRequest) {
        ArticleResponse articleResponse =articleService.createArticle(articleRequest);
        return ResponseEntity.ok(articleResponse);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleResponse> getBySlugForAdmin(@PathVariable String slug){
        return ResponseEntity.ok(articleService.findSlugForAdmin(slug));
    }


}
