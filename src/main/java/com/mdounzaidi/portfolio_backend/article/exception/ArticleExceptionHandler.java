package com.mdounzaidi.portfolio_backend.article.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice(basePackages = "com.mdounzaidi.portfolio_backend.article")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ArticleExceptionHandler {

    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleArticleNotFound(ArticleNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, "Article not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateArticleException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateArticle(DuplicateArticleException ex) {
        return buildProblem(HttpStatus.CONFLICT, "Duplicate article", ex.getMessage());
    }

    @ExceptionHandler(InvalidArticleStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArticleState(InvalidArticleStateException ex) {
        return buildProblem(HttpStatus.CONFLICT, "Invalid article state", ex.getMessage());
    }

    @ExceptionHandler(ArticleAuthorizationException.class)
    public ResponseEntity<ProblemDetail> handleArticleAuthorization(ArticleAuthorizationException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ResponseEntity<ProblemDetail> handleAccessDenied(Exception ex) {
        return buildProblem(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleRequestBodyValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );

        problem.setTitle("Validation error");
        problem.setType(URI.create("article/validation-error"));
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleRequestParamValidation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );

        problem.setTitle("Validation error");
        problem.setType(URI.create("article/validation-error"));
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    private ResponseEntity<ProblemDetail> buildProblem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("article/" + title.toLowerCase().replace(" ", "-")));

        return ResponseEntity.status(status).body(problem);
    }
}
