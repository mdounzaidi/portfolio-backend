package com.mdounzaidi.portfolio_backend.article.dto;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private long id;
    private String title;
    private String slug;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private ArticleStatus articleStatus;
}
