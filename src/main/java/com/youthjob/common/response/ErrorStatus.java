package com.youthjob.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorStatus {
    /**
     * 400 BAD_REQUEST
     */
    BAD_REQUEST_MISSING_PARAM(HttpStatus.BAD_REQUEST, "요청 값이 입력되지 않았습니다."),
    BAD_REQUEST_MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다."),
    BAD_REQUEST_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다."),
    BAD_REQUEST_DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    BAD_REQUEST_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    BAD_REQUEST_INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST,"이미지 확장자 업로드만 가능합니다"),
    BAD_REQUEST_DUPLICATE_PHONE(HttpStatus.BAD_REQUEST, "이미 사용 중인 전화번호입니다."),
    BAD_REQUEST_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "잘못된 이메일입니다."),
    BAD_REQUEST_POST_WRITER_NOT_SAME_USER(HttpStatus.BAD_REQUEST, "작성자와 요청자가 다릅니다."),
    BAD_REQUEST_NOT_ALLOW_IMG_MIME(HttpStatus.BAD_REQUEST, "이미지 파일(jpg, jpeg, png, bmp, webp)만 업로드할 수 있습니다."),
    BAD_REQUEST_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "자기 자신과는 채팅할 수 없습니다."),

    /**
     * 401 UNAUTHORIZED
     */
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED,"인증되지 않은 사용자입니다."),
    UNAUTHORIZED_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    UNAUTHORIZED_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    UNAUTHORIZED_UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "지원되지 않는 토큰입니다."),
    UNAUTHORIZED_EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 비어있습니다."),
    UNAUTHORIZED_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED_EMAIL_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "이메일 혹은 비밀번호를 다시 확인하세요."),

    /**
     * 403 FORBIDDEN
     */
    FORBIDDEN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /**
     * 404 NOT_FOUND
     */


    /**
     * 500 SERVER_ERROR
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),


    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}

