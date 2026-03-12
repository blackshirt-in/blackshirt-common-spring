package com.blackshirt.cms.common.exception;

import org.springframework.http.HttpStatus;

public abstract class CmsBaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public CmsBaseException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public CmsBaseException(String message, Throwable cause, String errorCode, HttpStatus status) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }
}
