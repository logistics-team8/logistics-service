package com.logistics.gateway.response;

import java.util.List;

public record ApiError(String code, String message, List<ValidationError> errors) {}
