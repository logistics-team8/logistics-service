package com.logistics.gateway.presentation.response;


import java.util.List;

public record ApiError(String code, String message, List<ValidationError> errors) {
}
