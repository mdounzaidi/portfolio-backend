package com.mdounzaidi.portfolio_backend.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 50000)
    private String content;
}
