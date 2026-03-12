package com.blackshirt.cms.common.exception;

import org.springframework.http.HttpStatus;

public class ContentServiceException extends CmsBaseException {

    public ContentServiceException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public ContentServiceException(String message, Throwable cause, String errorCode, HttpStatus status) {
        super(message, cause, errorCode, status);
    }
}
