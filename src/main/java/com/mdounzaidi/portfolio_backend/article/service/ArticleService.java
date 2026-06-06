package com.mdounzaidi.portfolio_backend.article.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleRequest;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleSummaryResponse;
import com.mdounzaidi.portfolio_backend.article.dto.ArticleUpdateRequest;
import com.mdounzaidi.portfolio_backend.article.entity.Article;
import com.mdounzaidi.portfolio_backend.article.entity.ArticleStatus;
import com.mdounzaidi.portfolio_backend.article.exception.ArticleAuthorizationException;
import com.mdounzaidi.portfolio_backend.article.exception.ArticleNotFoundException;
import com.mdounzaidi.portfolio_backend.article.exception.DuplicateArticleException;
import com.mdounzaidi.portfolio_backend.article.exception.InvalidArticleStateException;
import com.mdounzaidi.portfolio_backend.article.mapper.ArticleMapper;
import com.mdounzaidi.portfolio_backend.article.repository.ArticleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final AccountService accountService;

    public ArticleService(
            ArticleRepository articleRepository,
            ArticleMapper articleMapper,
            AccountService accountService
    ) {
        this.articleRepository = articleRepository;
        this.articleMapper = articleMapper;
        this.accountService = accountService;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional
    public ArticleResponse createArticle(ArticleRequest articleRequest) {
        Account currentAccount = accountService.getCurrentAccount();
        String title = articleRequest.getTitle().trim();
        String slug = generateUniqueSlug(title);

        Article article= Article.builder().
                content(articleRequest.getContent()).
                title(title).
                author(currentAccount).
                authorName(buildAuthorName(currentAccount)).
                slug(slug).
                build();

        Article savedArticle = saveArticle(article);
        return articleMapper.buildArticleResponse(savedArticle);

    }

    private String buildAuthorName(Account account) {
        String fullName = (StringUtils.hasText(account.getLastName()))
                ? account.getFirstName() + " " + account.getLastName()
                : account.getFirstName();

        if (StringUtils.hasText(fullName)) {
            return fullName;
        }

        return account.getUsername();
    }

    private String generateUniqueSlug(String title) {
        return generateUniqueSlug(title, null);
    }

    private String generateUniqueSlug(String title, String currentSlug) {
        String baseSlug = slugify(title);

        if (baseSlug.equals(currentSlug)) {
            return currentSlug;
        }

        String slug = baseSlug;
        int counter = 2;

        while (!slug.equals(currentSlug) && articleRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String slugify(String title) {
        String normalizedTitle = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String slug = normalizedTitle
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (!StringUtils.hasText(slug)) {
            throw new InvalidArticleStateException("Article title must contain letters or numbers");
        }

        return slug;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional(readOnly = true)
    public ArticleResponse findSlugForAdmin(String slug){
        Account currentAccount = accountService.getCurrentAccount();
        Article article = findArticleBySlug(slug);
        ensureCanManageArticle(currentAccount, article);
        return articleMapper.buildArticleResponse(article);
    }

    @Transactional(readOnly = true)
    public ArticleResponse findSlugForPublic(String slug) {
        Article article =articleRepository.findBySlugAndArticleStatus(normalizeSlug(slug), ArticleStatus.PUBLISHED)
                .orElseThrow(
                        ()-> new ArticleNotFoundException("Article not found")
                );
        return articleMapper.buildArticleResponse(article);
    }

    private String normalizeSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new ArticleNotFoundException("Article not found");
        }

        return slug.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> findPublishedArticles(int page, int size) {
        Pageable pageable = buildPageable(page, size, publishedSort());
        return articleRepository
                .findByArticleStatus(ArticleStatus.PUBLISHED, pageable)
                .map(articleMapper::buildArticleSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchArticleByKeyword(String keyword, int page, int size) {
        Pageable pageable = buildPageable(page, size, publishedSort());
        String trimmedKeyword = keyword == null ? "" : keyword.trim();

        if (trimmedKeyword.length() < 2) {
            return Page.empty(pageable);
        }

        return articleRepository
                .findByArticleStatusAndTitleContainingIgnoreCase(
                        ArticleStatus.PUBLISHED,
                        trimmedKeyword,
                        pageable
                )
                .map(articleMapper::buildArticleSummaryResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> findArticlesForAdmin(
            ArticleStatus status,
            String keyword,
            int page,
            int size
    ) {
        Account currentAccount = accountService.getCurrentAccount();
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<Article> articles;

        if (isAdmin(currentAccount)) {
            articles = findAllManagedArticles(status, trimmedKeyword, pageable);
        } else {
            articles = findWriterArticles(currentAccount, status, trimmedKeyword, pageable);
        }

        return articles.map(articleMapper::buildArticleSummaryResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional
    public ArticleResponse updateArticle(String slug, ArticleUpdateRequest request) {
        Account currentAccount = accountService.getCurrentAccount();
        Article article = findArticleBySlug(slug);
        ensureCanManageArticle(currentAccount, article);

        boolean titleProvided = request.getTitle() != null;
        boolean contentProvided = request.getContent() != null;

        if (!titleProvided && !contentProvided) {
            throw new InvalidArticleStateException("At least one article field must be provided");
        }

        if (titleProvided) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new InvalidArticleStateException("Article title must not be blank");
            }

            String title = request.getTitle().trim();
            if (!title.equals(article.getTitle())) {
                article.setTitle(title);
                article.setSlug(generateUniqueSlug(title, article.getSlug()));
            }
        }

        if (contentProvided) {
            if (!StringUtils.hasText(request.getContent())) {
                throw new InvalidArticleStateException("Article content must not be blank");
            }
            article.setContent(request.getContent());
        }

        return articleMapper.buildArticleResponse(saveArticle(article));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional
    public ArticleResponse publishArticle(String slug) {
        Article article = findArticleBySlug(slug);

        if (article.getArticleStatus() == ArticleStatus.PUBLISHED) {
            throw new InvalidArticleStateException("Article is already published");
        }

        article.setArticleStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());
        return articleMapper.buildArticleResponse(saveArticle(article));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional
    public ArticleResponse unpublishArticle(String slug) {
        Article article = findArticleBySlug(slug);

        if (article.getArticleStatus() == ArticleStatus.DRAFT) {
            throw new InvalidArticleStateException("Article is already a draft");
        }

        article.setArticleStatus(ArticleStatus.DRAFT);
        article.setPublishedAt(null);
        return articleMapper.buildArticleResponse(saveArticle(article));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_WRITER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Transactional
    public void deleteArticle(String slug) {
        Account currentAccount = accountService.getCurrentAccount();
        Article article = findArticleBySlug(slug);
        ensureCanManageArticle(currentAccount, article);

        if (!isAdmin(currentAccount) && article.getArticleStatus() == ArticleStatus.PUBLISHED) {
            throw new ArticleAuthorizationException("Writers cannot delete published articles");
        }

        articleRepository.delete(article);
    }

    private Page<Article> findAllManagedArticles(
            ArticleStatus status,
            String keyword,
            Pageable pageable
    ) {
        if (status != null && keyword != null) {
            return articleRepository.findByArticleStatusAndTitleContainingIgnoreCase(status, keyword, pageable);
        }
        if (status != null) {
            return articleRepository.findByArticleStatus(status, pageable);
        }
        if (keyword != null) {
            return articleRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        }
        return articleRepository.findAll(pageable);
    }

    private Page<Article> findWriterArticles(
            Account author,
            ArticleStatus status,
            String keyword,
            Pageable pageable
    ) {
        if (status != null && keyword != null) {
            return articleRepository.findByAuthorAndArticleStatusAndTitleContainingIgnoreCase(
                    author,
                    status,
                    keyword,
                    pageable
            );
        }
        if (status != null) {
            return articleRepository.findByAuthorAndArticleStatus(author, status, pageable);
        }
        if (keyword != null) {
            return articleRepository.findByAuthorAndTitleContainingIgnoreCase(author, keyword, pageable);
        }
        return articleRepository.findByAuthor(author, pageable);
    }

    private Article findArticleBySlug(String slug) {
        return articleRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));
    }

    private Article saveArticle(Article article) {
        try {
            return articleRepository.save(article);
        } catch (DataIntegrityViolationException ex) {
            if (isArticleSlugConflict(ex)) {
                throw new DuplicateArticleException("Article slug already exists");
            }
            throw ex;
        }
    }

    private boolean isArticleSlugConflict(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        if (mostSpecificCause == null || mostSpecificCause.getMessage() == null) {
            return false;
        }

        String message = mostSpecificCause.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("slug") || message.contains("uk_article_slug");
    }

    private void ensureCanManageArticle(Account currentAccount, Article article) {
        if (isAdmin(currentAccount)) {
            return;
        }

        if (article.getAuthor() == null || article.getAuthor().getId() != currentAccount.getId()) {
            throw new ArticleAuthorizationException("You cannot manage another writer's article");
        }
    }

    private boolean isAdmin(Account account) {
        return account.getAccountRole().contains(AccountRole.ROLE_ADMIN)
                || account.getAccountRole().contains(AccountRole.ROLE_SUPERADMIN);
    }

    private Pageable buildPageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        return PageRequest.of(safePage, safeSize, sort);
    }

    private Sort publishedSort() {
        return Sort.by(
                Sort.Order.desc("publishedAt"),
                Sort.Order.desc("createdAt")
        );
    }
}
