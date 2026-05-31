package com.mdounzaidi.portfolio_backend.account.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice(basePackages = "com.mdounzaidi.portfolio_backend.account")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountExceptionHandler {

    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateAccount(DuplicateAccountException ex) {
        return buildProblem(HttpStatus.CONFLICT, "Duplicate account", ex.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(AccountNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, "Account not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPassword(InvalidPasswordException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid password", ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(InvalidTokenException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid token", ex.getMessage());
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ProblemDetail> handleExpiredToken(ExpiredTokenException ex) {
        return buildProblem(HttpStatus.GONE, "Expired token", ex.getMessage());
    }

    @ExceptionHandler(InviteNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInvite(InviteNotValidException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid invite", ex.getMessage());
    }

    @ExceptionHandler(AccountStateException.class)
    public ResponseEntity<ProblemDetail> handleAccountState(AccountStateException ex) {
        return buildProblem(HttpStatus.CONFLICT, "Invalid account state", ex.getMessage());
    }

    @ExceptionHandler(AccountAuthorizationException.class)
    public ResponseEntity<ProblemDetail> handleAccountAuthorization(AccountAuthorizationException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
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
        problem.setType(URI.create("account/validation-error"));
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
        problem.setType(URI.create("account/validation-error"));
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
        problem.setType(URI.create("account/" + title.toLowerCase().replace(" ", "-")));

        return ResponseEntity.status(status).body(problem);
    }
}
