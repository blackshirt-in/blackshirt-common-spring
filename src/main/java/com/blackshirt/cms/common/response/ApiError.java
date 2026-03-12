package com.blackshirt.cms.common.response;

public record ApiError(
    String code,
    String message,
    String details
) {}
