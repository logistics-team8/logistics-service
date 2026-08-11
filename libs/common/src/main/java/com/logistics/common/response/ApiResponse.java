package com.logistics.common.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.logistics.common.error.ErrorCode;
import java.util.List;

public final class ApiResponse<T> {

    private final T data;
    private final ApiError error;

    @JsonCreator
    private ApiResponse(
            @JsonProperty T data,
            @JsonProperty ApiError error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, List<ValidationError> errors) {
        ApiError error = new ApiError(errorCode.code(), errorCode.message(), errors);
        return new ApiResponse<>(null, error);
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
