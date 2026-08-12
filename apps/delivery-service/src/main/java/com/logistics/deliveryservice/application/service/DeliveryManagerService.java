package com.logistics.deliveryservice.application.service;

import com.logistics.deliveryservice.application.command.DeliveryManagerCreateCommand;
import com.logistics.deliveryservice.application.dto.DeliveryManagerCreateResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerDetailResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerSearchResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerUpdateResponse;
import com.logistics.common.response.PageableUtil;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.port.UserSlackProvider;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerSearchRequest;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerUpdateRequest;
import com.logistics.common.security.principal.CustomUserDetails;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DeliveryManagerService {

    /** Redis **/
    // 마지막 발급 순번 저장용 Key 이름
    private static final String LAST_SEQUENCE_KEY_PREFIX =
            "delivery:manager:assignment:last-sequence:";
    // 분산 락(Distributed Lock) 제어용 Key
    private static final String LOCK_KEY_PREFIX = "delivery:manager:assignment:lock:";
    // 분산 락 획득 최대 대기 시간 (0.01초)
    private static final long LOCK_WAIT_NANOS = 10_000_000L;
    // 분산 락 해제용 Lua 스크립트 (락 소유권 검증 후 원자적 삭제 수행)
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "sequenceNumber",
            "createdAt",
            "updatedAt"
    );

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final UserSlackProvider userSlackProvider;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    // 서비스 작동에 필요한 의존성(DB, 슬랙, Redis, 트랜잭션 관리자) 주입 및 초기화
    public DeliveryManagerService(
            DeliveryManagerRepository deliveryManagerRepository,
            UserSlackProvider userSlackProvider,
            StringRedisTemplate redisTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.deliveryManagerRepository = deliveryManagerRepository;
        this.userSlackProvider = userSlackProvider;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DeliveryManagerCreateResponse create(DeliveryManagerCreateCommand command) {
        DeliveryManagerAssignmentGroup assignmentGroup = new DeliveryManagerAssignmentGroup(
                command.managerType(),
                command.hubId()
        );

        // 분산 락 안에서 안전하게 순번을 할당받아 배송 담당자를 검증 후 최종 생성/저장
        DeliveryManager deliveryManager = allocateAndPersist(
                assignmentGroup,
                sequenceNumber -> {
                    validateNotAlreadyRegistered(command); // 이미 등록된 사용자인지 검증
                    return deliveryManagerRepository.save(DeliveryManager.create(
                            command.userId(),
                            assignmentGroup.managerType(),
                            assignmentGroup.hubId(),
                            sequenceNumber
                    ));
                }
        );
        return DeliveryManagerCreateResponse.from(deliveryManager);
    }

    private void validateNotAlreadyRegistered(DeliveryManagerCreateCommand command) {
        if (deliveryManagerRepository.findByUserId(command.userId()).isPresent()) {
            throw new DeliveryException(DeliveryErrorCode.DUPLICATE_DELIVERY_MANAGER);
        }
    }

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

    /**
     * 배송 담당자 수정
     **/
    public DeliveryManagerUpdateResponse update(
            UUID userId,
            DeliveryManagerUpdateRequest request,
            CustomUserDetails userDetails
    ) {
        if (request.hasNoUpdateFields()) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
        }

        // 배송 담당자 존재 검증
        DeliveryManager deliveryManager = deliveryManagerRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 수정 정보(request) 있으면 채택 없으면 기존 값
        DeliveryManagerAssignmentGroup updatedAssignmentGroup = new DeliveryManagerAssignmentGroup(
                request.managerType() != null ? request.managerType() : deliveryManager.getManagerType(),
                request.hubId() != null ? request.hubId() : deliveryManager.getHubId()
        );

        // 업데이트 권한 검증
        validateUpdatePermission(deliveryManager, updatedAssignmentGroup, userDetails);

        // 배송 담당자의 소속 변경시, 분산 락 걸고 새 순번을 받아서 업데이트
        if (isAssignmentGroupChanged(deliveryManager, updatedAssignmentGroup)) {
            return DeliveryManagerUpdateResponse.from(allocateAndPersist( // 분산락 적용
                    updatedAssignmentGroup,
                    sequenceNumber -> updateManager(
                            userId,
                            updatedAssignmentGroup,
                            sequenceNumber
                    )
            ));
        }

        // 배송 담당자의 소속 변경 X, 기존 순번 유지하고 나머지만 업데이트
        return DeliveryManagerUpdateResponse.from(executeInTransaction(
                () -> updateManager(
                        userId,
                        updatedAssignmentGroup,
                        deliveryManager.getDeliverySequence()
                )
        ));
    }

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

    private void validateUpdatePermission(
            DeliveryManager deliveryManager,
            DeliveryManagerAssignmentGroup updatedAssignmentGroup,
            CustomUserDetails userDetails
    ) {
        if ("MASTER".equals(userDetails.getRole())) {
            return;
        }

        if ("HUB_MANAGER".equals(userDetails.getRole())
                && userDetails.getHubId() != null
                && userDetails.getHubId().equals(deliveryManager.getHubId())
                && userDetails.getHubId().equals(updatedAssignmentGroup.hubId())) {
            return;
        }

        throw new AccessDeniedException("배송 담당자 수정 권한이 없습니다.");
    }

    // 담당자 정보 실제로 변경됐는지 확인
    private boolean isAssignmentGroupChanged(
            DeliveryManager deliveryManager,
            DeliveryManagerAssignmentGroup updatedAssignmentGroup
    ) {
        DeliveryManagerAssignmentGroup currentAssignmentGroup = new DeliveryManagerAssignmentGroup(
                deliveryManager.getManagerType(),
                deliveryManager.getHubId()
        );
        // 생성된 두 그룹의 고유 식별 키(Key)가 일치하지 않는 경우에만 변경된 것으로 인식
        return !currentAssignmentGroup.assignmentGroupKey()
                .equals(updatedAssignmentGroup.assignmentGroupKey());
    }

    // 배송 담당자 정보 수정하고 저장
    private DeliveryManager updateManager(
            UUID userId,
            DeliveryManagerAssignmentGroup assignmentGroup,
            int sequenceNumber
    ) {
        DeliveryManager deliveryManager = deliveryManagerRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));
        deliveryManager.update(
                assignmentGroup.managerType(),
                assignmentGroup.hubId(),
                sequenceNumber
        );
        return deliveryManagerRepository.save(deliveryManager);
    }

    // Redis 분산 락 환경 제어 하에 안전하게 새로운 순번을 할당하고 비즈니스 로직
    private DeliveryManager allocateAndPersist(
            DeliveryManagerAssignmentGroup assignmentGroup,
            IntFunction<DeliveryManager> persistWithSequence
    ) {
        String lockKey = lockKey(assignmentGroup); // 분산 락용 고유 식별 Key 생성
        String lockValue = acquireLock(lockKey); // Redis 분산 락 획득 (선점 실패 시 대기)
        try {
            DeliveryManager deliveryManager = executeInTransaction(() -> {
                int sequenceNumber = findNextSequence(assignmentGroup);
                return persistWithSequence.apply(sequenceNumber);
            });
            redisTemplate.opsForValue().set(
                    lastSequenceKey(assignmentGroup),
                    String.valueOf(deliveryManager.getDeliverySequence())
            );
            return deliveryManager;
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    private DeliveryManager executeInTransaction(
            java.util.function.Supplier<DeliveryManager> operation
    ) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> operation.get()));
    }

    private int findNextSequence(DeliveryManagerAssignmentGroup assignmentGroup) {
        // 현재 그룹의 다른 직원들이 쓰고 있는 순번들 조회
        List<Integer> activeSequences = deliveryManagerRepository
                .findActiveDeliverySequences(assignmentGroup);
        if (activeSequences == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
        }

        Set<Integer> occupiedSequences = new HashSet<>();
        int maximumActiveSequence = DeliveryManagerAssignmentGroup.MIN_SEQUENCE;
        for (Integer activeSequence : activeSequences) {
            DeliveryManagerAssignmentGroup.validateSequence(activeSequence); // 0~9번 사이가 맞는지 검증
            occupiedSequences.add(activeSequence); // 이미 사용 중인 번호 목록(Set)에 추가
            maximumActiveSequence = Math.max(maximumActiveSequence, activeSequence); // 사용 중인 가장 큰 번호 기록
        }

        // 가장 마지막에 발급했던 번호가 몇 번이었는지 (그룹에 아무도 없으면 9, 있으면 큰 번호)
        int lastSequence = readOrInitializeLastSequence(
                assignmentGroup,
                activeSequences.isEmpty()
                        ? DeliveryManagerAssignmentGroup.MAX_SEQUENCE
                        : maximumActiveSequence
        );
        // 0->9->0 으로 돌면서 찾기
        for (int offset = 1; offset <= DeliveryManagerAssignmentGroup.MAX_MANAGER_COUNT; offset++) {
            int candidate = (lastSequence + offset) % DeliveryManagerAssignmentGroup.MAX_MANAGER_COUNT;
            if (!occupiedSequences.contains(candidate)) {
                return candidate; // 비어있는 가장 빠른 번호
            }
        }
        // 순번이 모두 차서 배정할 수 없는 경우 예외 처리
        throw new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_GROUP_FULL);
    }

    // Redis에서 마지막으로 발급했던 순번 읽어옴
    private int readOrInitializeLastSequence(
            DeliveryManagerAssignmentGroup assignmentGroup,
            int initializedSequence
    ) {
        String key = lastSequenceKey(assignmentGroup);
        String redisSequence = redisTemplate.opsForValue().get(key);
        // 캐시된 데이터가 존재하는 경우
        if (redisSequence != null) {
            try {
                int lastSequence = Integer.parseInt(redisSequence); // 정수 변환
                DeliveryManagerAssignmentGroup.validateSequence(lastSequence); // 0~9 범위 검증
                return lastSequence;
            } catch (NumberFormatException exception) {
                // 숫자가 아닌 잘못된 형식이 캐싱되어 있을 경우 예외 처리
                throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
            }
        }

        // Redis 조회 값 없을 때 최초 저장
        redisTemplate.opsForValue().set(key, String.valueOf(initializedSequence));
        return initializedSequence; // 저장한 값을 기본값으로 반환
    }

    private String acquireLock(String lockKey) {
        String lockValue = UUID.randomUUID().toString(); // 고유 키 생성
        // lockkey 비어있으면 lockValue 넣고 잠금 (열릴 때까지 대기)
        while (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue))) {
            LockSupport.parkNanos(LOCK_WAIT_NANOS);
        }
        return lockValue;
    }

    // 락 해제
    private void releaseLock(String lockKey, String lockValue) {
        redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockValue);
    }

    // 마지막 순번 저장 주소 생성
    private String lastSequenceKey(DeliveryManagerAssignmentGroup assignmentGroup) {
        return LAST_SEQUENCE_KEY_PREFIX + assignmentGroup.assignmentGroupKey();
    }

    // 동시성을 제어할 자물쇠의 최종 주소
    private String lockKey(DeliveryManagerAssignmentGroup assignmentGroup) {
        return LOCK_KEY_PREFIX + assignmentGroup.assignmentGroupKey();
    }

    // 클라이언트에서 보낸 정렬 기준 단어를 데이터베이스 엔티티의 실제 필드 이름에 맞게 변환
    private Pageable mapSortPropertiesToEntityProperties(Pageable pageable) {
        // 정렬 조건이 없으면 기본적으로 deliverySequence 오름차순 지정
        Sort mappedSort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Order.asc("deliverySequence"))
                : Sort.by(pageable.getSort().stream() // 있다면
                        .map(order -> "sequenceNumber".equals(order.getProperty())
                                ? order.withProperty("deliverySequence") // sequenceNumber를 deliverySequence로 변경
                                : order)
                        .toList());
        // 변환된 정렬 정보를 포함하여 새로운 PageRequest 객체 생성 후 반환
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                mappedSort
        );
    }
}
