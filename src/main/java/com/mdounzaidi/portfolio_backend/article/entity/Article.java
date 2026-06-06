package com.mdounzaidi.portfolio_backend.article.entity;


import com.mdounzaidi.portfolio_backend.account.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        schema = "article",
        name = "article",
        indexes = {
                @Index(
                        name = "idx_article_status_published_created",
                        columnList = "article_status, published_at, created_at"
                ),
                @Index(
                        name = "idx_article_author_status_created",
                        columnList = "author_account_id, article_status, created_at"
                )
        }
)
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(unique = true, nullable = false, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_account_id", nullable = false)
    private Account author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "article_status", nullable = false)
    @Builder.Default
    private ArticleStatus articleStatus =ArticleStatus.DRAFT;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        synchronizePublicationState();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        synchronizePublicationState();
    }

    private void synchronizePublicationState() {
        if (articleStatus == ArticleStatus.PUBLISHED && publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }

        if (articleStatus == ArticleStatus.DRAFT) {
            publishedAt = null;
        }
    }
}
