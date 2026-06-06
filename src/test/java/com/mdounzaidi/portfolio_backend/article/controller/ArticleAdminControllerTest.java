package com.mdounzaidi.portfolio_backend.article.controller;

import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.exception.ArticleExceptionHandler;
import com.mdounzaidi.portfolio_backend.article.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleAdminControllerTest {

    @Mock
    private ArticleService articleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArticleAdminController(articleService))
                .setControllerAdvice(new ArticleExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createArticle_shouldReturnCreatedAndLocation() throws Exception {
        when(articleService.createArticle(any())).thenReturn(
                ArticleResponse.builder().slug("test-article").title("Test Article").build()
        );

        mockMvc.perform(post("/api/articles")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Test Article",
                                  "content": "Content"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/articles/test-article"))
                .andExpect(jsonPath("$.slug").value("test-article"));
    }

    @Test
    void createArticle_shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));

        verify(articleService, never()).createArticle(any());
    }

    @Test
    void updateAndDelete_shouldReturnExpectedStatuses() throws Exception {
        when(articleService.updateArticle(any(), any())).thenReturn(
                ArticleResponse.builder().slug("updated").build()
        );

        mockMvc.perform(patch("/api/articles/test-article")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("updated"));

        mockMvc.perform(delete("/api/articles/test-article"))
                .andExpect(status().isNoContent());
    }
}
