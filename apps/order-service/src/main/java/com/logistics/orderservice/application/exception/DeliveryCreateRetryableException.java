package com.logistics.orderservice.application.exception;

/**
 * 연결 실패, 타임아웃, 502/503/504처럼 잠시 후 성공할 수 있고
 * 최초 요청의 처리 결과도 확인해야 하는 배송 생성 실패다.
 */
public class DeliveryCreateRetryableException extends DeliveryCreateException {

    public DeliveryCreateRetryableException(String message) {
        super(message);
    }

    public DeliveryCreateRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
