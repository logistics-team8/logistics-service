package com.logistics.common.security.hendler;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.error.ErrorCode;
import com.logistics.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;


import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final JsonMapper jsonMapper;
    private static final Logger log =
            LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    public CustomAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        ErrorCode errorCode = CommonErrorCode.FORBIDDEN;

        log.error(
                "ErrorCode : {}, ErrorMessage : {}",
                errorCode.code(),
                errorCode.message(),
                accessDeniedException);

        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        var errorResponse = ApiResponse.failure(errorCode);

        jsonMapper.writeValue(response.getWriter(), errorResponse);
    }
}
