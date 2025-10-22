package com.youthjob.common.exception;

import com.youthjob.common.response.ErrorStatus;
import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseException {

    public InternalServerException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}
