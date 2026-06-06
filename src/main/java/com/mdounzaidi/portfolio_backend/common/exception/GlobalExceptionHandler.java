package com.mdounzaidi.portfolio_backend.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleRequestBodyValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                "Request validation failed"
        );
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleRequestParamValidation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                "Request validation failed"
        );
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(buildProblem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "Request body is missing or invalid"
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(buildProblem(
                HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                "Required parameter is missing: " + ex.getParameterName()
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(buildProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter",
                "Invalid value for parameter: " + ex.getName()
        ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(buildProblem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "HTTP method is not supported for this endpoint"
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String detail = "Request conflicts with existing data";
        Throwable mostSpecificCause = ex.getMostSpecificCause();

        if (mostSpecificCause != null && mostSpecificCause.getMessage() != null) {
            String message = mostSpecificCause.getMessage().toLowerCase();

            if (message.contains("username")) {
                detail = "Username already exists";
            } else if (message.contains("email")) {
                detail = "Email already exists";
            } else if (message.contains("slug") || message.contains("uk_article_slug")) {
                detail = "Article slug already exists";
            }
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildProblem(
                HttpStatus.CONFLICT,
                "Data conflict",
                detail
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Unexpected server error"
        ));
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("error/" + title.toLowerCase().replace(" ", "-")));
        return problem;
    }
}
