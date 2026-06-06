package com.mdounzaidi.portfolio_backend.article.exception;

public class DuplicateArticleException extends RuntimeException {

    public DuplicateArticleException(String message) {
        super(message);
    }
}
