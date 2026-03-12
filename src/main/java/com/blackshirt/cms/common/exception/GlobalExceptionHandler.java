package com.blackshirt.cms.common.exception;

import com.blackshirt.cms.common.response.ApiError;
import com.blackshirt.cms.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CmsBaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleCmsBaseException(CmsBaseException ex) {
        logger.error("CmsBaseException caught: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getErrorCode(), ex.getMessage(), "Handled by CmsBaseException");
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unhandled Exception caught: ", ex);
        ApiResponse<Void> response = ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        java.util.List<ApiError> errors = new java.util.ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.add(new ApiError("VALIDATION_ERROR", error.getDefaultMessage(), error.getField())));
        return new ResponseEntity<>(ApiResponse.error(errors), HttpStatus.BAD_REQUEST);
    }
}
