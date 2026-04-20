package com.mdounzaidi.portfolio_backend.article.dto;

import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequest {
    private String title;
    private String content;
    private String authorName;
}
