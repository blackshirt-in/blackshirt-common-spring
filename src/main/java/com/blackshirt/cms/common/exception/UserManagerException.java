package com.blackshirt.cms.common.exception;

import org.springframework.http.HttpStatus;

public class UserManagerException extends CmsBaseException {

    public UserManagerException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public UserManagerException(String message, Throwable cause, String errorCode, HttpStatus status) {
        super(message, cause, errorCode, status);
    }
}
