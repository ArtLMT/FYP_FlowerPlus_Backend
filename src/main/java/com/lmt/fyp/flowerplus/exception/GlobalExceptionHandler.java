package com.lmt.fyp.flowerplus.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorResponse;
import com.lmt.fyp.flowerplus.module.auth.application.exception.EmailUsedException;
import com.lmt.fyp.flowerplus.module.user.application.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler capturing all standard and custom exceptions.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    //  1. Custom Business Exceptions (ApiException hierarchy)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("[{}] {} — path={}", ex.getCode().name(), ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(ex.getCode().getStatus().value())
                .errorCode(ex.getCode().name())
                .error(ex.getCode().getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, ex.getCode().getStatus());
    }

    // ------------------------------------------------------------------ //
    //  1b. Domain/Application exceptions (framework-free core)
    //      The clean-architecture core throws plain exceptions that know
    //      nothing about HTTP; the web layer maps them to a response here.
    // ------------------------------------------------------------------ //

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        log.warn("[USER_NOT_FOUND] {} — path={}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode(ErrorCode.USER_NOT_FOUND.name())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailUsed(
            EmailUsedException ex, HttpServletRequest request) {
        log.warn("[EMAIL_ALREADY_EXISTS] {} — path={}", ex.getMessage(), request.getRequestURI());

        HttpStatus status = ErrorCode.EMAIL_ALREADY_EXISTS.getStatus();
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(status.value())
                .errorCode(ErrorCode.EMAIL_ALREADY_EXISTS.name())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, status);
    }

    // ------------------------------------------------------------------ //
    //  2. Bean Validation Failures (@Valid / @Validated)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("[VALIDATION_FAILED] {} field errors — path={}", ex.getErrorCount(), request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((err) -> {
            String fieldName = ((FieldError) err).getField();
            String errorMessage = err.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .validationErrors(errors)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------ //
    //  3. Spring Security — Authentication failure (wrong credentials)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("[INVALID_CREDENTIALS] {} — path={}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(ErrorCode.INVALID_CREDENTIALS.name())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Invalid email or password")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ //
    //  3b. Spring Security — Account state rejection (blocked before the
    //      password is even checked, by SecurityUser's UserDetails flags).
    //
    //      Without this, both exceptions fall through to the generic handler
    //      below and a routine, expected rejection is reported as a 500 with a
    //      full stack trace.
    // ------------------------------------------------------------------ //

    @ExceptionHandler({LockedException.class, DisabledException.class})
    public ResponseEntity<ErrorResponse> handleAccountBlocked(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("[ACCOUNT_BLOCKED] {} — path={}", ex.getMessage(), request.getRequestURI());

        HttpStatus status = ErrorCode.ACCOUNT_BLOCKED.getStatus();
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(status.value())
                .errorCode(ErrorCode.ACCOUNT_BLOCKED.name())
                .error(status.getReasonPhrase())
                .message("This account is not permitted to sign in")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, status);
    }

    // ------------------------------------------------------------------ //
    //  4. Spring Security — Authorization failure (insufficient privileges)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[ACCESS_DENIED] {} — path={}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode(ErrorCode.ACCESS_DENIED.name())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ //
    //  5. Fallback — anything not matched above
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("[INTERNAL_ERROR] Unhandled exception — path={}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.INTERNAL_ERROR.name())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
