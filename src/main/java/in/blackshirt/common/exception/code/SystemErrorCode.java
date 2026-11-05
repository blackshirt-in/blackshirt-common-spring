package in.blackshirt.common.exception.code;

/**
 * System fallback and unhandled error codes.
 * Prefix: {@code ERR-SYS-}, Series Range: {@code 001..099}.
 */
public enum SystemErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR("ERR-SYS-001", "An unexpected system error occurred", 500),
    CRITICAL_FAILURE("ERR-SYS-002", "A critical system failure occurred", 500);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    SystemErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}

