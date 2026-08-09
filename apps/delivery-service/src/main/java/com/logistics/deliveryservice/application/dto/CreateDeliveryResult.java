package com.logistics.deliveryservice.application.dto;

/**
 * Controller가 최초 생성과 멱등 재요청의 HTTP 상태를 구분하도록 생성 여부를 함께 전달한다.
 */
public record CreateDeliveryResult(
        CreateDeliveryResponse response,
        boolean created
) {

    public static CreateDeliveryResult created(CreateDeliveryResponse response) {
        return new CreateDeliveryResult(response, true);
    }

    public static CreateDeliveryResult existing(CreateDeliveryResponse response) {
        return new CreateDeliveryResult(response, false);
    }
}
