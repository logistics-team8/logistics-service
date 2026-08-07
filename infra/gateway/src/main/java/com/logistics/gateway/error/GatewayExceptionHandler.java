package com.logistics.gateway.presentation.exception;

import com.logistics.gateway.error.ErrorCode;
import com.logistics.gateway.error.GatewayErrorCode;
import com.logistics.gateway.presentation.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Order(-2)
@Configuration
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    private final JsonMapper jsonMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        return handleException(exchange, throwable);
    }

    private Mono<Void> handleException(ServerWebExchange exchange, Throwable throwable) {
        ErrorCode errorCode;
        DataBuffer dataBuffer;

        if (throwable instanceof BusinessException e) {
            errorCode = e.getErrorCode();
        } else if (throwable instanceof ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                errorCode = GatewayErrorCode.RESOURCE_NOT_FOUND;
            } else {
                errorCode = GatewayErrorCode.INVALID_INPUT;
            }
        } else {
            errorCode = GatewayErrorCode.INTERNAL_SERVER_ERROR;
        }

        ApiResponse<Void> errorResponse = ApiResponse.failure(errorCode);

        byte[] bytes = jsonMapper.writeValueAsBytes(errorResponse);
        dataBuffer = exchange.getResponse().bufferFactory().wrap(bytes);

        exchange.getResponse().setStatusCode(errorCode.status());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }
}
