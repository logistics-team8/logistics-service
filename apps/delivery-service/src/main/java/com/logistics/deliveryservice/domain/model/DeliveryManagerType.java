package com.logistics.deliveryservice.domain.model;

/**
 * 배송 담당자가 허브 간 이동을 담당하는지, 최종 업체 배송을 담당하는지 구분한다.
 */
public enum DeliveryManagerType {

    HUB_DELIVERY,
    COMPANY_DELIVERY
}
