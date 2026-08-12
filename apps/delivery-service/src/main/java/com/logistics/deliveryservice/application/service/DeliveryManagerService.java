package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.command.DeliveryManagerCreateCommand;
import com.logistics.deliveryservice.application.dto.DeliveryManagerCreateResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerDetailResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerSearchResponse;
import com.logistics.common.response.PageableUtil;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.port.UserSlackProvider;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerSearchRequest;
import com.logistics.common.security.principal.CustomUserDetails;

import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryManagerService {

    // 정렬 허용할 필드 목록
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "sequenceNumber",
            "createdAt",
            "updatedAt"
    );

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final UserSlackProvider userSlackProvider;

    /**
     * 배송 담당자 생성
     **/
    public DeliveryManagerCreateResponse create(DeliveryManagerCreateCommand command) {
        // 담당자 유형과 허브 ID를 그룹화
        DeliveryManagerAssignmentGroup assignmentGroup = new DeliveryManagerAssignmentGroup(
                command.managerType(),
                command.hubId()
        );

        // 같은 사용자가 이미 배송 담당자로 등록되어 있지 않은지 확인한다.(중복 검사)
        validateNotAlreadyRegistered(command);

        // 미사용 가장 작은 순번 배정(순번 결정)
        int sequenceNumber = assignmentGroup.findSmallestAvailableSequence(
                deliveryManagerRepository.findActiveDeliverySequences(assignmentGroup)
        );
        DeliveryManager deliveryManager = DeliveryManager.create(
                command.userId(),
                assignmentGroup,
                sequenceNumber
        );

        // 저장
        return DeliveryManagerCreateResponse.from(
                deliveryManagerRepository.save(deliveryManager)
        );
    }

    // 동일한 userId의 배송 담당자가 이미 등록되어 있는지 확인하는 메서드
    private void validateNotAlreadyRegistered(DeliveryManagerCreateCommand command) {
        if (deliveryManagerRepository.findByUserId(command.userId()).isPresent()) {
            throw new DeliveryException(DeliveryErrorCode.DUPLICATE_DELIVERY_MANAGER);
        }
    }

    /**
     * 배송 담당자 목록 조회
     **/
    @Transactional(readOnly = true)
    public Page<DeliveryManagerSearchResponse> search(
            DeliveryManagerSearchRequest request,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        UUID hubId = request.hubId(); // 요청 허브
        // 허브 매니저인 경우 (아닌경우는 Master로 바로 통과)
        if ("HUB_MANAGER".equals(userDetails.getRole())) {
            UUID userHubId = userDetails.getHubId();
            if (userHubId == null || (hubId != null && !hubId.equals(userHubId))) {
                throw new AccessDeniedException("허브 매니저는 자신에게 지정된 허브만 접근할 수 있습니다.");
            }
            // 허브 매니저는 본인 소속의 허브 데이터만 조회하도록 고정
            hubId = userHubId;
        }

        // 페이지, 정렬 정책 적용
        Pageable normalizedPageable = PageableUtil.normalize(
                pageable,
                ALLOWED_SORT_PROPERTIES
        );
        Pageable entityPageable = mapSortPropertiesToEntityProperties(normalizedPageable);

        // 허브 매니저: 본인 소속 허브만 / 마스터: 모든 허브 (담당자 목록 조회)
        return deliveryManagerRepository.search(
                hubId,
                request.managerType(),
                entityPageable
        ).map(DeliveryManagerSearchResponse::from);
    }

    /**
     * 배송 담당자 단건 조회
     **/
    @Transactional(readOnly = true)
    public DeliveryManagerDetailResponse getByUserId(
            UUID userId,
            CustomUserDetails userDetails
    ) {
        DeliveryManager deliveryManager = deliveryManagerRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 담당자 역할 권한 확인
        validateDetailReadPermission(deliveryManager, userDetails);

        String slackId = userSlackProvider.getSlackId(deliveryManager.getUserId());
        return DeliveryManagerDetailResponse.from(deliveryManager, slackId);
    }

    // 담당자 역할 권한 확인
    private void validateDetailReadPermission(
            DeliveryManager deliveryManager,
            CustomUserDetails userDetails
    ) {
        // TODO : permitAll 어떻게 할지
        if ("MASTER".equals(userDetails.getRole())) {
            return;
        }

        if ("HUB_MANAGER".equals(userDetails.getRole())
                && userDetails.getHubId() != null
                && userDetails.getHubId().equals(deliveryManager.getHubId())) {
            return;
        }

        if ("DELIVERY_MANAGER".equals(userDetails.getRole())
                && userDetails.getId().equals(deliveryManager.getUserId())) {
            return;
        }

        throw new AccessDeniedException("배송 담당자 조회 권한이 없습니다.");
    }



    // API 정렬 필드명을 Entity 정렬 필드명으로 변환
    private Pageable mapSortPropertiesToEntityProperties(Pageable pageable) {
        // 필드명 같은지 다른지 삼항연산으로 비교 변환
        Sort mappedSort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Order.asc("deliverySequence"))
                : Sort.by(pageable.getSort().stream()
                        .map(order -> "sequenceNumber".equals(order.getProperty())
                                ? order.withProperty("deliverySequence")
                                : order)
                        .toList());
        //order 안에는 대략 이렇게 들어있음
        //├─ property  = "sequenceNumber"
        //└─ direction = DESC

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                mappedSort
        );
    }
}
