package in.blackshirt.common.exception.code;

/**
 * Database, SQL, and persistence error codes.
 * Prefix: {@code ERR-DB-}, Series Range: {@code 001..099}.
 */
public enum DatabaseErrorCode implements ErrorCode {

    DATABASE_ERROR("ERR-DB-001", "A database persistence error occurred", 500),
    DATA_INTEGRITY_VIOLATION("ERR-DB-002", "A database data integrity constraint was violated", 409),
    DUPLICATE_KEY("ERR-DB-003", "A duplicate record already exists with the provided unique identifier", 409),
    LOCK_FAILURE("ERR-DB-004", "Database lock conflict occurred – please retry the request", 409),
    QUERY_TIMEOUT("ERR-DB-005", "Database query execution timed out", 504),
    CONNECTION_FAILURE("ERR-DB-006", "Could not establish database connection", 503);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    DatabaseErrorCode(String code, String defaultMessage, int httpStatus) {
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

