package com.youthjob.common.response;

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

    // === HRD/저장 과정 관련 ===
    HRD_COURSE_SEARCH_SUCCESS(HttpStatus.OK, "훈련과정 조회 성공"),
    HRD_SAVED_LIST_SUCCESS(HttpStatus.OK, "관심 훈련과정 목록 조회 성공"),
    HRD_SAVED_ADD_SUCCESS(HttpStatus.OK, "관심 훈련과정 저장 성공"),
    HRD_SAVED_DELETE_SUCCESS(HttpStatus.OK, "관심 훈련과정 삭제 성공"),
    HRD_SAVED_TOGGLE_SUCCESS(HttpStatus.OK, "관심 훈련과정 토글 성공"),

    // === JOB(채용) 관련 ===
    JOB_SEARCH_SUCCESS(HttpStatus.OK, "채용공고 조회 성공"),
    JOB_GET_SUCCESS(HttpStatus.OK, "채용공고 단건 조회 성공"),
    JOB_UPSERT_SUCCESS(HttpStatus.OK, "채용공고 저장 성공"),

    JOB_SAVED_ADD_SUCCESS(HttpStatus.OK, "관심 채용 저장 성공"),
    JOB_SAVED_DELETE_SUCCESS(HttpStatus.OK, "관심 채용 삭제 성공"),
    JOB_SAVED_TOGGLE_SUCCESS(HttpStatus.OK, "관심 채용 토글 성공"),
    JOB_SAVED_LIST_SUCCESS(HttpStatus.OK, "관심 채용 목록 조회 성공"),
    JOB_SAVED_EXISTS_SUCCESS(HttpStatus.OK, "관심 채용 여부 조회 성공"),
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

