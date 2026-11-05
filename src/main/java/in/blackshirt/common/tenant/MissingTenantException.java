package in.blackshirt.common.tenant;

import in.blackshirt.common.exception.BaseException;
import in.blackshirt.common.exception.code.HttpErrorCode;

/**
 * Thrown when an incoming request is missing a mandatory tenant identifier.
 */
public class MissingTenantException extends BaseException {

    public MissingTenantException(String headerName) {
        super(HttpErrorCode.BAD_REQUEST, "Missing required tenant identifier in request header: " + headerName);
        addDetails("headerName", headerName);
    }
}
