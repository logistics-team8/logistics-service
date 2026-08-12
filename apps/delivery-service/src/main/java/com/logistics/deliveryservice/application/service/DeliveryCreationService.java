package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.command.DeliveryCreateCommand;
import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryPlan;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당자 배정과 Delivery Aggregate 저장을 한 트랜잭션에서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class DeliveryCreationService {

    private final DeliveryManagerAssignmentService assignmentService;
    private final DeliveryRepository deliveryRepository;

    /**
     * Cursor 잠금이 커밋까지 유지되도록 배정, 생성, 저장을 같은 트랜잭션에서 수행한다.
     */
    @Transactional
    public Delivery register(DeliveryCreateCommand command, DeliveryPlan deliveryPlan) {
        DeliveryPlan assignedPlan = assignmentService.assignManagers(
                deliveryPlan,
                command.arrivalHubId()
        );
        Delivery delivery = Delivery.create(
                command.orderId(),
                command.requesterId(),
                command.departureHubId(),
                command.arrivalHubId(),
                command.deliveryAddress(),
                command.receiverName(),
                command.receiverSlackId(),
                assignedPlan
        );
        return deliveryRepository.save(delivery);
    }
}
