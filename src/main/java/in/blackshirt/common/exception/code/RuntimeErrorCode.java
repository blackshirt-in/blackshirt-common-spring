package in.blackshirt.common.exception.code;

/**
 * Core Java runtime, language-level, and execution state error codes.
 * Prefix: {@code ERR-RT-}, Series Range: {@code 001..099}.
 */
public enum RuntimeErrorCode implements ErrorCode {

    NULL_POINTER("ERR-RT-001", "A null reference error occurred during request processing", 500),
    ILLEGAL_ARGUMENT("ERR-RT-002", "Invalid argument passed", 400),
    ILLEGAL_STATE("ERR-RT-003", "Application is in an invalid state for the requested operation", 409),
    ARITHMETIC_ERROR("ERR-RT-004", "An arithmetic calculation error occurred", 400),
    INDEX_OUT_OF_BOUNDS("ERR-RT-005", "Requested index is out of bounds", 400),
    UNSUPPORTED_OPERATION("ERR-RT-006", "The requested operation is not supported", 501);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    RuntimeErrorCode(String code, String defaultMessage, int httpStatus) {
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

