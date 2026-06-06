package com.mdounzaidi.portfolio_backend.article.controller;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleSummaryResponse;
import com.mdounzaidi.portfolio_backend.article.exception.ArticleExceptionHandler;
import com.mdounzaidi.portfolio_backend.article.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticlePublicControllerTest {

    @Mock
    private ArticleService articleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArticlePublicController(articleService))
                .setControllerAdvice(new ArticleExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void findPublishedArticles_shouldReturnSummaryPage() throws Exception {
        when(articleService.findPublishedArticles(0, 10)).thenReturn(
                new PageImpl<>(List.of(
                        ArticleSummaryResponse.builder().title("Published").slug("published").build()
                ), PageRequest.of(0, 10), 1)
        );

        mockMvc.perform(get("/api/public/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("published"));
    }

    @Test
    void getBySlugForPublic_shouldReturnArticle() throws Exception {
        when(articleService.findSlugForPublic("published")).thenReturn(
                ArticleResponse.builder().slug("published").build()
        );

        mockMvc.perform(get("/api/public/articles/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("published"));
    }

    @Test
    void search_shouldReturnSummaryPage() throws Exception {
        when(articleService.searchArticleByKeyword("spring", 0, 10)).thenReturn(
                new PageImpl<>(List.of(
                        ArticleSummaryResponse.builder().title("Spring").slug("spring").build()
                ), PageRequest.of(0, 10), 1)
        );

        mockMvc.perform(get("/api/public/articles/search").param("keyword", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("spring"));
    }
}
