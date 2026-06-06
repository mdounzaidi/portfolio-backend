package com.mdounzaidi.portfolio_backend.article.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleUpdateRequest {

    @Size(max = 150)
    private String title;

    @Size(max = 50000)
    private String content;
}
