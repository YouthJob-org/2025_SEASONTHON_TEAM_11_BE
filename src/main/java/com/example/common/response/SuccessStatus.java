package com.example.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    /**
     * 200 OK
     */
    LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공"),
    LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃 성공"),
    MEMBER_GET_SUCCESS(HttpStatus.OK, "회원 정보 조회 성공"),
    MEMBER_RESIGN_DELETE_SUCCESS(HttpStatus.OK, "회원탈퇴 성공"),
    MEMBER_INFO_GET_SUCCESS(HttpStatus.OK, "현재 사용자 정보 조회 성공"),
    ARTICLE_GET_SUCCESS(HttpStatus.OK,"게시글 조회 성공"),
    ARTICLE_UPDATE_SUCCESS(HttpStatus.OK,"게시글 수정 성공"),
    MEMBER_SIGNUP_SUCCESS(HttpStatus.OK,"회원가입 성공"),
    AUTH_SUCCESS(HttpStatus.OK, "인증에 성공했습니다."),


    /**
     * 201 CREATED
     */


    /**
     * 204 NO CONTENT
     */



    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}

