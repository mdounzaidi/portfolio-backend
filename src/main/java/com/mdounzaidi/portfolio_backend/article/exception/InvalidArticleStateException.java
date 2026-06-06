package com.mdounzaidi.portfolio_backend.article.exception;

public class InvalidArticleStateException extends RuntimeException {

    public InvalidArticleStateException(String message) {
        super(message);
    }
}
