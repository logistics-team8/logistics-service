package com.logistics.orderservice.application.exception;

/**
 * 잘못된 요청, 인증·권한, 충돌처럼 같은 요청을 다시 보내도
 * 성공하지 않는 명확한 배송 생성 실패다.
 */
public class DeliveryCreateRejectedException extends DeliveryCreateException {

    public DeliveryCreateRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
