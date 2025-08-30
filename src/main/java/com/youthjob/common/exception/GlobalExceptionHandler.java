package com.youthjob.common.exception;

import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.ErrorStatus;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 커스텀 예외(BaseException) */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBase(BaseException ex) {
        final int code = ex.getStatusCode();
        final String msg = ex.getResponseMessage() != null
                ? ex.getResponseMessage()
                : HttpStatus.valueOf(code).getReasonPhrase();
        return ResponseEntity.status(code).body(ApiResponse.fail(code, msg));
    }

    /** 인증 실패 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity
                .status(ErrorStatus.UNAUTHORIZED_USER.getStatusCode())
                .body(ApiResponse.failOnly(ErrorStatus.UNAUTHORIZED_USER));
    }

    /** 권한 거부 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(ErrorStatus.FORBIDDEN_ACCESS_DENIED.getStatusCode())
                .body(ApiResponse.failOnly(ErrorStatus.FORBIDDEN_ACCESS_DENIED));
    }

    /** @Valid 바인딩 오류 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        String msg;
        if (ex instanceof MethodArgumentNotValidException manv) {
            msg = manv.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else {
            BindException be = (BindException) ex;
            msg = be.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), msg));
    }

    /** 파라미터 제약 위반 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), msg));
    }

    /** 마지막 안전망 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception ex) {
        return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(ApiResponse.failOnly(ErrorStatus.INTERNAL_SERVER_ERROR));
    }
}
