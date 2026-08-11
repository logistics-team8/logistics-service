package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.command.CreateDeliveryCommand;
import com.logistics.deliveryservice.application.dto.CreateDeliveryResponse;
import com.logistics.deliveryservice.application.dto.CreateDeliveryResult;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.port.HubDeliveryPlanProvider;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 주문 기반 배송 생성과 order_id 멱등성 판정을 조정한다.
 */
@Service
@RequiredArgsConstructor
public class CreateDeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final HubDeliveryPlanProvider hubDeliveryPlanProvider;

    /**
     * 기존 배송을 먼저 확인하고, 신규 주문일 때만 Hub 계획 조회와 Aggregate 저장을 수행한다.
     */
    public CreateDeliveryResult create(CreateDeliveryCommand command) {
        // 동일 orderId 재요청에서는 Hub 조회와 저장을 반복하지 않도록 기존 배송부터 확인한다.
        Optional<Delivery> existingDelivery = deliveryRepository.findByOrderId(command.orderId());
        if (existingDelivery.isPresent()) {
            return resolveExisting(existingDelivery.get(), command);
        }

        // 신규 주문일 때만 Hub Service에서 담당자와 전체 허브 이동 계획을 조회한다.
        DeliveryPlan deliveryPlan = hubDeliveryPlanProvider.getDeliveryPlan(
                command.orderId(),
                command.departureHubId(),
                command.arrivalHubId()
        );
        // Hub 계획의 무결성을 검증하면서 Delivery와 모든 Route를 하나의 Aggregate로 생성한다.
        Delivery delivery = Delivery.create(
                command.orderId(),
                command.requesterId(),
                command.departureHubId(),
                command.arrivalHubId(),
                command.deliveryAddress(),
                command.receiverName(),
                command.receiverSlackId(),
                deliveryPlan
        );

        try {
            // Aggregate Root를 저장하면 Cascade 설정에 따라 소속 Route도 함께 저장된다.
            Delivery savedDelivery = deliveryRepository.save(delivery);
            return CreateDeliveryResult.created(CreateDeliveryResponse.from(savedDelivery));
        } catch (DataIntegrityViolationException exception) {
            // 조회와 저장 사이 다른 요청이 먼저 생성했다면 재조회하여 멱등 또는 충돌로 판정한다.
            return resolveConcurrentCreation(command, exception);
        }
    }

    private CreateDeliveryResult resolveConcurrentCreation(
            CreateDeliveryCommand command,
            DataIntegrityViolationException exception
    ) {
        // 경합 상대가 만든 배송이 없으면 order_id 외 제약 위반으로 보고 원본 예외를 유지한다.
        return deliveryRepository.findByOrderId(command.orderId())
                .map(delivery -> resolveExisting(delivery, command))
                .orElseThrow(() -> exception);
    }

    private CreateDeliveryResult resolveExisting(
            Delivery existingDelivery,
            CreateDeliveryCommand command
    ) {
        // 취소·삭제된 배송 또는 불변 요청값이 다른 배송은 같은 주문의 멱등 요청으로 볼 수 없다.
        if (existingDelivery.isRecreationBlocked()
                || !existingDelivery.hasSameImmutablePayload(
                        command.requesterId(),
                        command.departureHubId(),
                        command.arrivalHubId(),
                        command.deliveryAddress(),
                        command.receiverName(),
                        command.receiverSlackId()
                )) {
            throw new DeliveryException(DeliveryErrorCode.DUPLICATE_ORDER_DELIVERY);
        }

        // 불변 요청값까지 같으면 중복 생성하지 않고 기존 배송을 정상 응답으로 반환한다.
        return CreateDeliveryResult.existing(CreateDeliveryResponse.from(existingDelivery));
    }
}
