package com.youthjob.common.exception;

import com.youthjob.common.response.ErrorStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseException extends RuntimeException {

    HttpStatus statusCode;
    String responseMessage;

    public BaseException(HttpStatus statusCode) {
        super();
        this.statusCode = statusCode;
    }

    public BaseException(HttpStatus statusCode, String responseMessage) {
        super(responseMessage);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
    }

    public BaseException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.statusCode = errorStatus.getHttpStatus();
        this.responseMessage = errorStatus.getMessage();
    }

    public int getStatusCode() {
        return this.statusCode.value();
    }
}

