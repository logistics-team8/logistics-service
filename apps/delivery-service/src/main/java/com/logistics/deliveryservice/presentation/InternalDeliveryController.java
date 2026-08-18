package com.logistics.deliveryservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.deliveryservice.application.dto.DeliveryCreateResponse;
import com.logistics.deliveryservice.application.dto.DeliveryCreateResult;
import com.logistics.deliveryservice.application.dto.DeliveryGetByOrderResponse;
import com.logistics.deliveryservice.application.service.DeliveryService;
import com.logistics.deliveryservice.presentation.dto.DeliveryCancelRequest;
import com.logistics.deliveryservice.presentation.dto.DeliveryCreateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order Service가 사용하는 Delivery 내부 API를 제공한다.
 */
@RestController
@RequestMapping("/internal/v1/deliveries")
@RequiredArgsConstructor
public class InternalDeliveryController {

    private final DeliveryService deliveryService;

    /**
     * 최초 요청은 201, 동일한 멱등 재요청은 기존 배송과 함께 200을 반환한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryCreateResponse>> createDelivery(
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        // HTTP 요청 DTO를 Application Command로 바꿔 생성 유스케이스를 실행한다.
        DeliveryCreateResult result = deliveryService.create(request.toCommand());
        // 최초 생성은 201, 동일 요청의 멱등 응답은 기존 리소스를 반환하므로 200을 사용한다.
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(result.response()));
    }

    /**
     * 주문 ID에 연결된 활성 배송과 계획된 Route 목록을 반환한다.
     */
    @GetMapping("/by-order/{orderId}")
    public ApiResponse<DeliveryGetByOrderResponse> getDeliveryByOrder(
            @PathVariable UUID orderId
    ) {
        // HTTP 경로의 주문 ID를 조회 유스케이스로 전달하고 공통 성공 응답으로 감싼다.
        DeliveryGetByOrderResponse response = deliveryService.getByOrderId(orderId);
        return ApiResponse.success(response);
    }

    /**
     * 주문 취소에 맞춰 활성 배송 상태만 CANCELED로 변경한다.
     */
    @PostMapping("/by-order/{orderId}/cancel")
    public ResponseEntity<Void> cancelDeliveryByOrder(
            @PathVariable UUID orderId,
            @RequestBody DeliveryCancelRequest request
    ) {
        // cancelReason은 현재 저장하지 않는 요청 계약 필드이므로 Service로 전달하지 않는다.
        deliveryService.cancelByOrderId(orderId);
        return ResponseEntity.noContent().build();
    }
}
