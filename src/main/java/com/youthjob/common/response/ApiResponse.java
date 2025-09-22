// src/main/java/com/youthjob/common/response/ApiResponse.java
package com.youthjob.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final boolean success;
    private final String message;
    private T data;

    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessStatus status, T data) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .status(status.getStatusCode())
                .success(true)
                .message(status.getMessage())
                .data(data)
                .build();
        return ResponseEntity.status(status.getStatusCode()).body(response);
    }

    public static ResponseEntity<ApiResponse<Void>> successOnly(SuccessStatus status) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(status.getStatusCode())
                .success(true)
                .message(status.getMessage())
                .build();
        return ResponseEntity.status(status.getStatusCode()).body(response);
    }

    // ---- 실패 응답: 제네릭/ResponseEntity까지 감싸는 빌더 추가 ----
    public static <T> ResponseEntity<ApiResponse<T>> failResponse(ErrorStatus status) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .status(status.getStatusCode())
                .success(false)
                .message(status.getMessage())
                .build();
        return ResponseEntity.status(status.getStatusCode()).body(response);
    }

    // 필요 시 메시지 override 버전
    public static <T> ResponseEntity<ApiResponse<T>> failResponse(ErrorStatus status, String message) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .status(status.getStatusCode())
                .success(false)
                .message(message == null ? status.getMessage() : message)
                .build();
        return ResponseEntity.status(status.getStatusCode()).body(response);
    }

    // (기존) 바디만 만드는 실패 객체 — 그대로 둬도 됨
    public static ApiResponse<Void> failOnly(ErrorStatus status) {
        return ApiResponse.<Void>builder()
                .status(status.getStatusCode())
                .success(false)
                .message(status.getMessage())
                .build();
    }

    // (호환 유지용) 상태코드/메시지 직접 지정 버전
    public static ApiResponse<Void> fail(int status, String message) {
        return ApiResponse.<Void>builder()
                .status(status)
                .success(false)
                .message(message)
                .build();
    }
}
