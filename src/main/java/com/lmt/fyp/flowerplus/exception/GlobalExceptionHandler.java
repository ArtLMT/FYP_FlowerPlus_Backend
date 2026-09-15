package com.lmt.fyp.flowerplus.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorDetails;
import com.lmt.fyp.flowerplus.common.dto.ErrorResponse;
import com.lmt.fyp.flowerplus.module.auth.exception.EmailUsedException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpAttemptsExceededException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpDailyLimitReachedException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpInvalidException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpThrottledException;
import com.lmt.fyp.flowerplus.module.user.exception.AddressLimitReachedException;
import com.lmt.fyp.flowerplus.module.user.exception.AddressNotFoundException;
import com.lmt.fyp.flowerplus.module.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Global exception handler capturing all standard and custom exceptions.
 * Every body is built by {@link ErrorResponse#of}, never field by field.
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

        return respond(ErrorResponse.of(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    // ------------------------------------------------------------------ //
    //  1b. Module exceptions that carry no HTTP knowledge
    //      Services throw plain domain exceptions; the web layer maps each to
    //      its status and response shape here.
    // ------------------------------------------------------------------ //

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        log.warn("[USER_NOT_FOUND] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(ErrorCode.USER_NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    /** 404 not 403: a 403 would confirm the row exists. */
    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAddressNotFound(
            AddressNotFoundException ex, HttpServletRequest request) {
        log.warn("[ADDRESS_NOT_FOUND] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(ErrorCode.ADDRESS_NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AddressLimitReachedException.class)
    public ResponseEntity<ErrorResponse> handleAddressLimitReached(
            AddressLimitReachedException ex, HttpServletRequest request) {
        log.warn("[ADDRESS_LIMIT_REACHED] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(ErrorCode.ADDRESS_LIMIT_REACHED, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(EmailUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailUsed(
            EmailUsedException ex, HttpServletRequest request) {
        log.warn("[EMAIL_ALREADY_EXISTS] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * OTP rejections. All of them are routine, expected outcomes of a public
     * endpoint, so they are mapped here rather than being left to the catch-all
     * below, which would report a mistyped code as a 500 with a stack trace.
     *
     * <p>Grouped like {@code handleAccountBlocked}: one method, one shape of
     * response, with only the error code varying. Both throttle codes also say
     * how long to wait, so the client never has to parse the message.
     */
    @ExceptionHandler({
            OtpInvalidException.class,
            OtpAttemptsExceededException.class,
            OtpThrottledException.class
    })
    public ResponseEntity<ErrorResponse> handleOtpFailure(
            RuntimeException ex, HttpServletRequest request) {

        ErrorCode code = switch (ex) {
            case OtpAttemptsExceededException ignored -> ErrorCode.OTP_ATTEMPTS_EXCEEDED;
            case OtpDailyLimitReachedException ignored -> ErrorCode.OTP_DAILY_LIMIT_REACHED;
            case OtpThrottledException ignored -> ErrorCode.OTP_THROTTLED;
            default -> ErrorCode.OTP_INVALID;
        };
        ErrorDetails details = ex instanceof OtpThrottledException throttled
                ? new ErrorDetails.Retry(throttled.getRetryAfter().toSeconds())
                : null;

        log.warn("[{}] {} — path={}", code.name(), ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(code, ex.getMessage(), request.getRequestURI(), details));
    }

    // ------------------------------------------------------------------ //
    //  2. Bean Validation Failures (@Valid / @Validated)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("[VALIDATION_FAILED] {} field errors — path={}", ex.getErrorCount(), request.getRequestURI());

        List<ErrorDetails.FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ErrorDetails.FieldViolation(
                        err.getField(), err.getCode(), err.getDefaultMessage()))
                .toList();

        return respond(ErrorResponse.of(
                ErrorCode.VALIDATION_FAILED,
                "Validation failed",
                request.getRequestURI(),
                new ErrorDetails.Validation(fields)));
    }

    // ------------------------------------------------------------------ //
    //  2b. Malformed request — bad parameter type or unreadable body
    //      Client mistakes, not user input: no field to show a message on.
    //      Left to the catch-all they surface as a 500.
    // ------------------------------------------------------------------ //

    /**
     * A path id that cannot be parsed (e.g. /api/addresses/not-a-uuid) names
     * nothing, so it is a 404 like an unknown URL. A bad query parameter is a
     * broken request, so it stays a 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        // Fixed messages: never echo the raw value or the parser's detail.
        if (ex.getParameter().hasParameterAnnotation(PathVariable.class)) {
            log.warn("[NOT_FOUND] path variable '{}' has an invalid value — path={}",
                    ex.getName(), request.getRequestURI());
            return respond(ErrorResponse.of(
                    ErrorCode.NOT_FOUND, "No resource at this path", request.getRequestURI()));
        }

        log.warn("[MALFORMED_REQUEST] parameter '{}' has an invalid value — path={}",
                ex.getName(), request.getRequestURI());
        return respond(ErrorResponse.of(
                ErrorCode.MALFORMED_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value",
                request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[MALFORMED_REQUEST] unreadable request body — path={}", request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.MALFORMED_REQUEST,
                "Request body is missing or malformed",
                request.getRequestURI()));
    }

    // ------------------------------------------------------------------ //
    //  2c. Request matches no endpoint — unknown path, wrong method, wrong
    //      content type. Spring signals these by throwing, so without these
    //      handlers the catch-all below reports them as 500s with a stack trace.
    // ------------------------------------------------------------------ //

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoEndpoint(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("[NOT_FOUND] no endpoint — path={}", request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.NOT_FOUND, "No endpoint at this path", request.getRequestURI()));
    }

    /** HTTP requires a 405 to list the methods the path does support, in Allow. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("[METHOD_NOT_ALLOWED] {} — path={}", ex.getMethod(), request.getRequestURI());

        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return respond(ErrorResponse.of(
                ErrorCode.METHOD_NOT_ALLOWED,
                "HTTP method not supported for this path",
                request.getRequestURI()), headers);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("[UNSUPPORTED_MEDIA_TYPE] {} — path={}", ex.getContentType(), request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "Content type not supported; send application/json",
                request.getRequestURI()));
    }

    // ------------------------------------------------------------------ //
    //  3. Spring Security — Authentication failure (wrong credentials)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("[INVALID_CREDENTIALS] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", request.getRequestURI()));
    }

    // ------------------------------------------------------------------ //
    //  3b. Spring Security — Account state rejection (blocked before the
    //      password is even checked, by SecurityUser's UserDetails flags).
    //
    //      Without this, both exceptions fall through to the generic handler
    //      below and a routine, expected rejection is reported as a 500 with a
    //      full stack trace.
    // ------------------------------------------------------------------ //

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountBlocked(
            LockedException ex, HttpServletRequest request) {
        log.warn("[ACCOUNT_BLOCKED] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.ACCOUNT_BLOCKED, "This account is not permitted to sign in", request.getRequestURI()));
    }

    /**
     * A PENDING (email-unverified) account failing the {@code isEnabled} check.
     * Split out from the locked/banned case above so the user is told to verify
     * their email rather than that their account is blocked.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotVerified(
            DisabledException ex, HttpServletRequest request) {
        log.warn("[ACCOUNT_NOT_VERIFIED] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.ACCOUNT_NOT_VERIFIED,
                "Please verify your email before signing in. Check your inbox for the code.",
                request.getRequestURI()));
    }

    // ------------------------------------------------------------------ //
    //  4. Spring Security — Authorization failure (insufficient privileges)
    // ------------------------------------------------------------------ //

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[ACCESS_DENIED] {} — path={}", ex.getMessage(), request.getRequestURI());

        return respond(ErrorResponse.of(
                ErrorCode.ACCESS_DENIED,
                "You do not have permission to access this resource",
                request.getRequestURI()));
    }

    // ------------------------------------------------------------------ //
    //  5. Fallback — anything not matched above
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("[INTERNAL_ERROR] Unhandled exception — path={}", request.getRequestURI(), ex);

        return respond(ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request.getRequestURI()));
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
        return ResponseEntity.status(body.status()).body(body);
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorResponse body, HttpHeaders headers) {
        return ResponseEntity.status(body.status()).headers(headers).body(body);
    }
}
