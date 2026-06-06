package com.mdounzaidi.portfolio_backend.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolation_shouldReturnUsernameConflict_whenUsernameConstraintFails() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate value",
                new RuntimeException("duplicate key value violates unique constraint accounts_username_key")
        );

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data conflict", response.getBody().getTitle());
        assertEquals("Username already exists", response.getBody().getDetail());
    }

    @Test
    void handleDataIntegrityViolation_shouldReturnEmailConflict_whenEmailConstraintFails() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate value",
                new RuntimeException("duplicate key value violates unique constraint accounts_email_key")
        );

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data conflict", response.getBody().getTitle());
        assertEquals("Email already exists", response.getBody().getDetail());
    }

    @Test
    void handleDataIntegrityViolation_shouldReturnArticleSlugConflict_whenSlugConstraintFails() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate value",
                new RuntimeException("duplicate key value violates unique constraint uk_article_slug")
        );

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data conflict", response.getBody().getTitle());
        assertEquals("Article slug already exists", response.getBody().getDetail());
    }

    @Test
    void handleDataIntegrityViolation_shouldReturnGenericConflict_whenConstraintIsUnknown() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate value",
                new RuntimeException("foreign key constraint failed")
        );

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data conflict", response.getBody().getTitle());
        assertEquals("Request conflicts with existing data", response.getBody().getDetail());
    }
}
