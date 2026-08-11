package com.logistics.deliveryservice.domain.exception;

import com.logistics.common.exception.BusinessException;

/**
 * Delivery 도메인과 애플리케이션 규칙 위반을 공통 예외 처리기로 전달한다.
 */
public class DeliveryException extends BusinessException {

    public DeliveryException(DeliveryErrorCode errorCode) {
        super(errorCode);
    }

    public DeliveryException(DeliveryErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
