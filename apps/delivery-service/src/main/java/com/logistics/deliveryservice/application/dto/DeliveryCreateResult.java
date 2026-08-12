package com.logistics.deliveryservice.application.dto;

/**
 * Controller가 최초 생성과 멱등 재요청의 HTTP 상태를 구분하도록 생성 여부를 함께 전달한다.
 */
public record DeliveryCreateResult(
        DeliveryCreateResponse response,
        boolean created
) {

    public static DeliveryCreateResult created(DeliveryCreateResponse response) {
        return new DeliveryCreateResult(response, true);
    }

    public static DeliveryCreateResult existing(DeliveryCreateResponse response) {
        return new DeliveryCreateResult(response, false);
    }
}
