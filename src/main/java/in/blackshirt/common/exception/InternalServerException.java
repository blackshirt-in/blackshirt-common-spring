package in.blackshirt.common.exception;

import in.blackshirt.common.response.ErrorCode;

public class InternalServerException extends BaseException {

    protected InternalServerException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
