package com.mdounzaidi.portfolio_backend.article.controller;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleRequest;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleSummaryResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleUpdateRequest;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import com.mdounzaidi.portfolio_backend.article.service.ArticleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@RestController
@RequestMapping("/api/articles")
@Validated
public class ArticleAdminController {

    private final ArticleService articleService;

    public ArticleAdminController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<Page<ArticleSummaryResponse>> findArticlesForAdmin(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(articleService.findArticlesForAdmin(status, keyword, page, size));
    }

    @PostMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody ArticleRequest articleRequest) {
        ArticleResponse articleResponse =articleService.createArticle(articleRequest);
        return ResponseEntity
                .created(URI.create("/api/articles/" + articleResponse.getSlug()))
                .body(articleResponse);
    }

    @GetMapping("/{slug}")
    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<ArticleResponse> getBySlugForAdmin(@PathVariable @NotBlank String slug){
        return ResponseEntity.ok(articleService.findSlugForAdmin(slug));
    }

    @PatchMapping("/{slug}")
    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable @NotBlank String slug,
            @Valid @RequestBody ArticleUpdateRequest articleUpdateRequest
    ) {
        return ResponseEntity.ok(articleService.updateArticle(slug, articleUpdateRequest));
    }

    @PatchMapping("/{slug}/publish")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<ArticleResponse> publishArticle(@PathVariable @NotBlank String slug) {
        return ResponseEntity.ok(articleService.publishArticle(slug));
    }

    @PatchMapping("/{slug}/unpublish")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<ArticleResponse> unpublishArticle(@PathVariable @NotBlank String slug) {
        return ResponseEntity.ok(articleService.unpublishArticle(slug));
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<Void> deleteArticle(@PathVariable @NotBlank String slug) {
        articleService.deleteArticle(slug);
        return ResponseEntity.noContent().build();
    }

}
