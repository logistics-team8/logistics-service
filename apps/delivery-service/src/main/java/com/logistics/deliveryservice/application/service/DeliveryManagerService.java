package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.command.CreateDeliveryManagerCommand;
import com.logistics.deliveryservice.application.dto.CreateDeliveryManagerResponse;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.port.DeliveryManagerHubValidator;
import com.logistics.deliveryservice.domain.port.DeliveryManagerUserValidator;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryManagerService {

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryManagerUserValidator deliveryManagerUserValidator;
    // TODO : 추후 구현체 구현
//    private final DeliveryManagerHubValidator deliveryManagerHubValidator;

    public CreateDeliveryManagerResponse create(CreateDeliveryManagerCommand command) {
        // 담당자 유형과 허브 ID를 그룹화
        DeliveryManagerAssignmentGroup assignmentGroup = new DeliveryManagerAssignmentGroup(
                command.managerType(),
                command.hubId()
        );

        // userId가 배송 담당자가 맞는지 검증
        deliveryManagerUserValidator.validateDeliveryManager(command.userId());
        // 허브가 존재하는지 검증
//        deliveryManagerHubValidator.validateActiveHub(command.hubId());

        // 같은 사용자가 이미 배송 담당자로 등록되어 있지 않은지 확인한다.
        validateNotAlreadyRegistered(command);

        // 미사용 가장 작은 순번 배정
        int sequenceNumber = assignmentGroup.findSmallestAvailableSequence(
                deliveryManagerRepository.findActiveDeliverySequences(assignmentGroup)
        );
        DeliveryManager deliveryManager = DeliveryManager.create(
                command.userId(),
                assignmentGroup,
                sequenceNumber
        );

        return CreateDeliveryManagerResponse.from(
                deliveryManagerRepository.save(deliveryManager)
        );
    }

    // 동일한 userId의 배송 담당자가 이미 등록되어 있는지 확인하는 메서드
    private void validateNotAlreadyRegistered(CreateDeliveryManagerCommand command) {
        if (deliveryManagerRepository.findByUserId(command.userId()).isPresent()) {
            throw new DeliveryException(DeliveryErrorCode.DUPLICATE_DELIVERY_MANAGER);
        }
    }
}
